package com.daluobai.jenkinslib.codeup

import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.TernaryExpression
import org.codehaus.groovy.ast.expr.UnaryMinusExpression
import org.codehaus.groovy.ast.expr.UnaryPlusExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilePhase

class JenkinsfileInvocationParser implements Serializable {

    Map parse(String jenkinsfileContent, Set<String> allowedMethods) {
        if (jenkinsfileContent == null || jenkinsfileContent.trim().isEmpty()) {
            throw new IllegalArgumentException('Jenkinsfile内容空的')
        }
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            throw new IllegalArgumentException('allowedMethods空的')
        }

        String sanitized = stripLibraryDeclaration(jenkinsfileContent)
        List nodes = new AstBuilder().buildFromString(CompilePhase.CONVERSION, false, sanitized)
        BlockStatement block = nodes.find { it instanceof BlockStatement } as BlockStatement
        if (block == null) {
            throw new IllegalArgumentException('Jenkinsfile解析失败')
        }

        List<Statement> statements = block.statements.findAll { Statement statement ->
            !(statement instanceof EmptyStatement)
        }
        if (statements.size() != 2) {
            throw new IllegalArgumentException('Jenkinsfile仅支持一个customConfig定义和一个方法调用')
        }

        Map customConfig = parseCustomConfigStatement(statements[0])
        String methodName = parseInvocationStatement(statements[1], allowedMethods)
        return [methodName: methodName, customConfig: customConfig]
    }

    private static String stripLibraryDeclaration(String content) {
        List<String> sanitizedLines = []
        boolean skipNextUnderscoreLine = false
        content.readLines().each { String line ->
            String trimmed = line.trim()
            if (skipNextUnderscoreLine && trimmed == '_') {
                skipNextUnderscoreLine = false
                return
            }
            if (trimmed.startsWith('@Library')) {
                skipNextUnderscoreLine = !trimmed.endsWith('_')
                return
            }
            skipNextUnderscoreLine = false
            sanitizedLines.add(line)
        }
        return sanitizedLines.join('\n')
    }

    private static Map parseCustomConfigStatement(Statement statement) {
        if (!(statement instanceof ExpressionStatement)) {
            throw new IllegalArgumentException('customConfig定义不合法')
        }

        Expression expression = ((ExpressionStatement) statement).expression
        Expression rightExpression
        String variableName
        if (expression instanceof DeclarationExpression) {
            DeclarationExpression declaration = (DeclarationExpression) expression
            variableName = declaration.leftExpression instanceof VariableExpression ? ((VariableExpression) declaration.leftExpression).name : null
            rightExpression = declaration.rightExpression
        } else if (expression instanceof BinaryExpression && expression.operation?.text == '=') {
            BinaryExpression binary = (BinaryExpression) expression
            variableName = binary.leftExpression instanceof VariableExpression ? ((VariableExpression) binary.leftExpression).name : null
            rightExpression = binary.rightExpression
        } else {
            throw new IllegalArgumentException('customConfig定义不合法')
        }

        if (variableName != 'customConfig') {
            throw new IllegalArgumentException('仅支持变量customConfig')
        }
        Object value = parseLiteralValue(rightExpression)
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException('customConfig必须是字面量Map')
        }
        return (Map) value
    }

    private static String parseInvocationStatement(Statement statement, Set<String> allowedMethods) {
        if (!(statement instanceof ExpressionStatement)) {
            throw new IllegalArgumentException('方法调用不合法')
        }

        Expression expression = ((ExpressionStatement) statement).expression
        if (!(expression instanceof MethodCallExpression)) {
            throw new IllegalArgumentException('方法调用不合法')
        }

        MethodCallExpression methodCall = (MethodCallExpression) expression
        if (!(methodCall.objectExpression instanceof VariableExpression) || ((VariableExpression) methodCall.objectExpression).name != 'this') {
            throw new IllegalArgumentException('仅支持顶层方法调用')
        }

        String methodName = methodCall.methodAsString
        if (methodName == null || !allowedMethods.contains(methodName)) {
            throw new IllegalArgumentException("不支持的方法: ${methodName}")
        }

        if (!(methodCall.arguments instanceof ArgumentListExpression)) {
            throw new IllegalArgumentException('方法参数不合法')
        }
        List<Expression> arguments = ((ArgumentListExpression) methodCall.arguments).expressions
        if (arguments.size() != 1 || !(arguments[0] instanceof VariableExpression) || ((VariableExpression) arguments[0]).name != 'customConfig') {
            throw new IllegalArgumentException('方法调用仅支持传入customConfig')
        }
        return methodName
    }

    private static Object parseLiteralValue(Expression expression) {
        if (expression instanceof ConstantExpression) {
            return ((ConstantExpression) expression).value
        }
        if (expression instanceof MapExpression) {
            Map<Object, Object> result = [:]
            ((MapExpression) expression).mapEntryExpressions.each { MapEntryExpression entry ->
                Object key = parseMapKey(entry.keyExpression)
                result.put(key, parseLiteralValue(entry.valueExpression))
            }
            return result
        }
        if (expression instanceof ListExpression) {
            return ((ListExpression) expression).expressions.collect { Expression item ->
                return parseLiteralValue(item)
            }
        }
        if (expression instanceof UnaryMinusExpression) {
            Object value = parseLiteralValue(((UnaryMinusExpression) expression).expression)
            if (value instanceof Number) {
                return -value
            }
            throw new IllegalArgumentException('仅支持数字字面量')
        }
        if (expression instanceof UnaryPlusExpression) {
            Object value = parseLiteralValue(((UnaryPlusExpression) expression).expression)
            if (value instanceof Number) {
                return value
            }
            throw new IllegalArgumentException('仅支持数字字面量')
        }
        if (expression instanceof CastExpression) {
            return parseLiteralValue(((CastExpression) expression).expression)
        }
        if (expression instanceof GStringExpression) {
            throw new IllegalArgumentException('不支持GString或字符串插值')
        }
        if (expression instanceof VariableExpression) {
            throw new IllegalArgumentException('customConfig中不支持变量引用')
        }
        if (expression instanceof MethodCallExpression) {
            throw new IllegalArgumentException('customConfig中不支持方法调用')
        }
        if (expression instanceof PropertyExpression) {
            throw new IllegalArgumentException('customConfig中不支持属性访问')
        }
        if (expression instanceof ClosureExpression) {
            throw new IllegalArgumentException('customConfig中不支持闭包')
        }
        if (expression instanceof TernaryExpression || expression instanceof BooleanExpression) {
            throw new IllegalArgumentException('customConfig中不支持条件表达式')
        }
        throw new IllegalArgumentException("customConfig中存在不支持的表达式: ${expression.getClass().simpleName}")
    }

    private static Object parseMapKey(Expression expression) {
        if (expression instanceof ConstantExpression) {
            return ((ConstantExpression) expression).value
        }
        if (expression instanceof VariableExpression) {
            return ((VariableExpression) expression).name
        }
        throw new IllegalArgumentException('customConfig中Map key仅支持字面量或标识符')
    }
}
