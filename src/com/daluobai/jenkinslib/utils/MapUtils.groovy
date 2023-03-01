package com.daluobai.jenkinslib.utils

import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic

class MapUtils implements Serializable {
    static boolean isMap(object){
        return object in Map
    }

    static Map pruneNulls(Map m) {

        Map result = [:]

        m = m ?: [:]

        m.each { key, value ->
            if(isMap(value))
                result[key] = pruneNulls(value)
            else if(value != null)
                result[key] = value
        }
        return result
    }

    /**
     * Merge two maps with the second one has precedence
     * @param base First map
     * @param overlay Second map, takes precedence
     * @return The merged map
     */
    static Map merge(Map base, Map overlay) {

        Map result = [:]

        base = base ?: [:]

        result.putAll(base)

        overlay.each { key, value ->
            result[key] = isMap(value) ? merge(base[key], value) : value
        }
        return result
    }

    /**
     * 合并map，从右向左
     * @param maps
     * @return
     */
    static def merge(List<Map> maps) {
        def injectMap = [:]
        for (i in 0..<maps.size()) {
            if (maps[i] == null){
                continue
            }
            injectMap = merge(injectMap, maps[i])
        }
        return injectMap
    }

    /**
     * @param m The map to which the changed denoted by closure <code>strategy</code>
     *        should be applied.
     *        The strategy is also applied to all sub-maps contained as values
     *        in <code>m</code> in a recursive manner.
     * @param strategy Strategy applied to all non-map entries
     */
    static void traverse(Map m, Closure strategy) {

        def updates = [:]
        m.each { key, value ->
            if(isMap(value)) {
                traverse(value, strategy)
            } else {
                // do not update the map while it is traversed. Depending
                // on the map implementation the behavior is undefined.
                updates.put(key, strategy(value))
            }
        }
        m.putAll(updates)
    }

    static private def getByPath(Map m, def key) {
        List path = key in CharSequence ? key.tokenize('/') : key

        def value = m.get(path.head())

        if (path.size() == 1) return value
        if (value in Map) return getByPath(value, path.tail())

        return null
    }

    /*
     * Provides a new map with the same content like the original map.
     * Nested Collections and Maps are copied. Values with are not
     * Collections/Maps are not copied/cloned.
     * &lt;paranoia&gt;&/ltThe keys are also not copied/cloned, even if they are
     * Maps or Collections;paranoia&gt;
     */
    static deepCopy(Map original) {
        Map copy = [:]
        for (def e : original.entrySet()) {
            if(e.value == null) {
                copy.put(e.key, e.value)
            } else {
                copy.put(e.key, deepCopy(e.value))
            }
        }
        copy
    }

    /* private */ static deepCopy(Set original) {
        Set copy = []
        for(def e : original)
            copy << deepCopy(e)
        copy
    }

    /* private */ static deepCopy(List original) {
        List copy = []
        for(def e : original)
            copy << deepCopy(e)
        copy
    }

    /*
     * In fact not a copy, but a catch all for everything not matching
     * with the other signatures
     */
    /* private */ static deepCopy(def original) {
        original
    }

    /**
     * groovy map字符串转 map
     * @param mapString [key1:value1, key2:value2]
     */
    static def mapString2Map(String mapString) {
//        def map =
//                // Take the String value between
//                // the [ and ] brackets.
//                mapString[1..-2]
//                // Split on , to get a List.
//                        .split(', ')
//                // Each list item is transformed
//                // to a Map entry with key/value.
//                        .collectEntries { entry ->
//                            def pair = entry.split(':')
//                            [(pair.first()): pair.last()]
//                        }
        def map = new JsonSlurper().parseText(mapString)
        return map
    }

    /**
     * groovy map Json字符串转 map
     * @param mapString {key1:value1, key2:value2}
     */
    static def mapJsonString2Map(String mapJsonString) {
        def map = new JsonSlurperClassic().parseText(mapJsonString)
        return map
    }
}
