package com.daluobai.jenkinslib.api

@Grab('cn.hutool:hutool-all:5.8.11')
@Grab('com.aliyun:tea-openapi:0.0.19')
@Grab('com.aliyun:alidns20150109:2.0.1')
import cn.hutool.http.HttpUtil
import cn.hutool.core.lang.Assert
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import cn.hutool.core.util.StrUtil
import com.aliyun.tea.*;
import com.aliyun.teaconsole.*;
import com.aliyun.teautil.*;
import com.aliyun.teaopenapi.*;
import com.aliyun.teaopenapi.models.*;
import com.aliyun.darabonba.env.*;
import com.aliyun.alidns20150109.*;
import com.aliyun.alidns20150109.models.*;

class AliDnsApi implements Serializable {
    def steps
    def com.aliyun.alidns20150109.Client client;

    AliDnsApi(steps) { this.steps = steps }

//    static{
//        this.client = this.Init()
//    }

    /**
     * Init 初始化客户端
     * @param accessKeyId
     * @param accessKeySecret
     * @return Client
     * @throws Exception
     */
    public static com.aliyun.alidns20150109.Client Init(String accessKeyId, String accessKeySecret, String regionId) throws Exception {
        Config config = new Config();
        // 传AccessKey ID入config
        config.accessKeyId = accessKeyId;
        // 传AccessKey Secret入config
        config.accessKeySecret = accessKeySecret;
        config.regionId = regionId;
        return new com.aliyun.alidns20150109.Client(config);
    }

    /**
     * DescribeDomains  查询账户下域名
     * @param client      客户端
     * @throws Exception
     */
    public static void DescribeDomains(com.aliyun.alidns20150109.Client client) throws Exception {
        DescribeDomainsRequest req = new DescribeDomainsRequest();
        com.aliyun.teaconsole.Client.log("查询域名列表(json)↓");
        try {
            DescribeDomainsResponse resp = client.describeDomains(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * AddDomain  阿里云云解析添加域名
     * @param client      客户端
     * @param domainName  域名名称
     * @throws Exception
     */
    public static void AddDomain(com.aliyun.alidns20150109.Client client, String domainName) throws Exception {
        AddDomainRequest req = new AddDomainRequest();
        req.domainName = domainName;
        com.aliyun.teaconsole.Client.log("云解析添加域名(" + domainName + ")的结果(json)↓");
        try {
            AddDomainResponse resp = client.addDomain(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * DescribeDomainRecords 查询域名解析记录
     * @param client          客户端
     * @param domainName      域名名称
     * @throws Exception
     */
    public static void DescribeDomainRecords(com.aliyun.alidns20150109.Client client, String domainName) throws Exception {
        DescribeDomainRecordsRequest req = new DescribeDomainRecordsRequest();
        req.domainName = domainName;
        com.aliyun.teaconsole.Client.log("查询域名(" + domainName + ")的解析记录(json)↓");
        try {
            DescribeDomainRecordsResponse resp = client.describeDomainRecords(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * DescribeRecordLogs  查询域名解析记录日志
     * @param client         客户端
     * @param domainName     域名名称
     * @throws Exception
     */
    public static void DescribeRecordLogs(com.aliyun.alidns20150109.Client client, String domainName) throws Exception {
        DescribeRecordLogsRequest req = new DescribeRecordLogsRequest();
        req.domainName = domainName;
        com.aliyun.teaconsole.Client.log("查询域名(" + domainName + ")的解析记录日志(json)↓");
        try {
            DescribeRecordLogsResponse resp = client.describeRecordLogs(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * DescribeDomainRecordByRecordId 查询域名解析记录信息
     * @param client         客户端
     * @param RecordId       解析记录id
     * @throws Exception
     */
    public static void DescribeDomainRecordByRecordId(com.aliyun.alidns20150109.Client client, String recordId) throws Exception {
        DescribeDomainRecordInfoRequest req = new DescribeDomainRecordInfoRequest();
        req.recordId = recordId;
        com.aliyun.teaconsole.Client.log("查询RecordId:" + recordId + "的域名解析记录信息(json)↓");
        try {
            DescribeDomainRecordInfoResponse resp = client.describeDomainRecordInfo(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * DescribeDomainInfo  查询域名信息
     * @param client         客户端
     * @param domainName     域名名称
     * @throws Exception
     */
    public static void DescribeDomainInfo(com.aliyun.alidns20150109.Client client, String domainName) throws Exception {
        DescribeDomainInfoRequest req = new DescribeDomainInfoRequest();
        req.domainName = domainName;
        com.aliyun.teaconsole.Client.log("查询域名:" + domainName + "的信息(json)↓");
        try {
            DescribeDomainInfoResponse resp = client.describeDomainInfo(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * AddDomainRecord  添加域名解析记录
     * @param client            客户端
     * @param domainName        域名名称
     * @param RR                主机记录
     * @param recordType              记录类型(A/NS/MX/TXT/CNAME/SRV/AAAA/CAA/REDIRECT_URL/FORWARD_URL)
     * @param value             记录值
     * @throws Exception
     */
    public static void AddDomainRecord(com.aliyun.alidns20150109.Client client, String domainName, String RR, String recordType, String Value) throws Exception {
        AddDomainRecordRequest req = new AddDomainRecordRequest();
        req.domainName = domainName;
        req.RR = RR;
        req.type = recordType;
        req.value = Value;
        try {
            AddDomainRecordResponse resp = client.addDomainRecord(req);
            com.aliyun.teaconsole.Client.log("添加域名解析记录的结果(json)↓");
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * UpdateDomainRecord  更新域名解析记录
     * @param client          客户端
     * @param recordId        解析记录ID
     * @param RR              主机记录
     * @param recordType            记录类型(A/NS/MX/TXT/CNAME/SRV/AAAA/CAA/REDIRECT_URL/FORWARD_URL)
     * @param value           记录值
     * @throws Exception
     */
    public static void UpdateDomainRecord(com.aliyun.alidns20150109.Client client, String recordId, String RR, String recordType, String Value) throws Exception {
        UpdateDomainRecordRequest req = new UpdateDomainRecordRequest();
        req.recordId = recordId;
        req.RR = RR;
        req.type = recordType;
        req.value = Value;
        com.aliyun.teaconsole.Client.log("更新域名解析记录的结果(json)↓");
        try {
            UpdateDomainRecordResponse resp = client.updateDomainRecord(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * SetDomainRecordStatus  设置域名解析状态
     * @param client      客户端
     * @param recordId    解析记录ID
     * @param status      解析状态(ENABLE/DISABLE)
     * @throws Exception
     */
    public static void SetDomainRecordStatus(com.aliyun.alidns20150109.Client client, String recordId, String status) throws Exception {
        SetDomainRecordStatusRequest req = new SetDomainRecordStatusRequest();
        req.recordId = recordId;
        req.status = status;
        com.aliyun.teaconsole.Client.log("设置域名解析状态的结果(json)↓");
        try {
            SetDomainRecordStatusResponse resp = client.setDomainRecordStatus(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * DeleteDomainRecord  删除域名解析记录
     * @param client         客户端
     * @param recordId       解析记录ID
     * @throws Exception
     */
    public static void DeleteDomainRecord(com.aliyun.alidns20150109.Client client, String recordId) throws Exception {
        DeleteDomainRecordRequest req = new DeleteDomainRecordRequest();
        req.recordId = recordId;
        com.aliyun.teaconsole.Client.log("删除域名解析记录的结果(json)↓");
        try {
            DeleteDomainRecordResponse resp = client.deleteDomainRecord(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * DescribeDomainGroups  查询域名组
     * @param client 客户端
     * @throws Exception
     */
    public static void DescribeDomainGroups(com.aliyun.alidns20150109.Client client) throws Exception {
        DescribeDomainGroupsRequest req = new DescribeDomainGroupsRequest();
        com.aliyun.teaconsole.Client.log("查询域名组(json)↓");
        try {
            DescribeDomainGroupsResponse resp = client.describeDomainGroups(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * AddDomainGroup  添加域名组
     * @param client      客户端
     * @param groupName   域名组名
     * @throws Exception
     */
    public static void AddDomainGroup(com.aliyun.alidns20150109.Client client, String groupName) throws Exception {
        AddDomainGroupRequest req = new AddDomainGroupRequest();
        req.groupName = groupName;
        com.aliyun.teaconsole.Client.log("添加域名组的结果(json)↓");
        try {
            AddDomainGroupResponse resp = client.addDomainGroup(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * UpdateDomainGroup  更新域名组名称
     * @param client         客户端
     * @param groupId        解析组ID
     * @param groupName      新域名组名称
     * @throws Exception
     */
    public static void UpdateDomainGroup(com.aliyun.alidns20150109.Client client, String groupId, String groupName) throws Exception {
        UpdateDomainGroupRequest req = new UpdateDomainGroupRequest();
        req.groupId = groupId;
        req.groupName = groupName;
        com.aliyun.teaconsole.Client.log("更新域名组的结果(json)↓");
        try {
            UpdateDomainGroupResponse resp = client.updateDomainGroup(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    /**
     * DeleteDomainGroup  删除域名组
     * @param client      客户端
     * @param groupId     域名组ID
     * @throws Exception
     */
    public static void DeleteDomainGroup(com.aliyun.alidns20150109.Client client, String groupId) throws Exception {
        DeleteDomainGroupRequest req = new DeleteDomainGroupRequest();
        req.groupId = groupId;
        com.aliyun.teaconsole.Client.log("删除域名组的结果(json)↓");
        try {
            DeleteDomainGroupResponse resp = client.deleteDomainGroup(req);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(resp)));
        } catch (TeaException error) {
            com.aliyun.teaconsole.Client.log(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            com.aliyun.teaconsole.Client.log(error.message);
        }
    }

    static void main(String[] args) {

    }

}