// Our audit logs are on this format
type CloudWatchData = {
  messageType: string
  owner: string
  logGroup: string
  logStream: string
  subscriptionFilters: string[]
  logEvents: {
    id: string
    timestamp: number

    // '{"@timestamp":"2025-09-01T08:58:32.193304792Z","log":{"level":"INFO","logger":"auditLogger"},"process":{"pid":1,"thread":{"name":"task-2"}},"service":{"name":"kitu","version":"0.0.1-SNAPSHOT","node":{}},"message":"Kielitesti suoritus viewed","suoritus":{"id":4},"principal":{"oid":"1.2.246.562.24.52694023470"},"ecs":{"version":"8.11"}}\n'
    message: string
  }[]
}
// CloudWatchData.logEvents.message
type KituLogEvent = {
  "@timestamp": string // "2025-09-01T07:19:33.694326330Z",
  log: any // { "level":"INFO", "logger":"auditLogger" },
  process: any // { "pid":1, "thread":{ "name":"HEARTBEAT-0" } },
  service: any // { "name":"kitu", "version":"0.0.1-SNAPSHOT", "node":{} },
  message: string
  ecs: any // {"version":"8.11"}}
}

// The data we are trying to send to koski
type AuditLogEntry = {
  version: number
  logSeq: number
  type: string // 'log',
  // TODO: environment
  bootTime: string // '2025-09-01T12:35:21.690+03',
  hostname: string // '',
  timestamp: string // '2025-09-01T12:44:01.425+03',
  serviceName: string // 'kitu',
  applicationType: string // 'backend',
  user: {
    oid: string // '1.2.246.562.24.52694023470',
    ip: string // '1.2.3.4',
    session: "01234567890ABCDEF0123456789ABCDEF"
    userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:142.0) Gecko/20100101 Firefox/142.0"
  }
  operation: "KielitestiSuoritusViewed"
  target: { oppijaHenkiloOid: "1.2.246.562.198.88975028874" }
  // TODO: organizationOid
  // TODO: operation
  changes: []
}

// The data should be sent on this format
type KoskiAuditLogEntry = {
  version: number
  logSeq: number
  bootTime: string
  type: string
  environment: string
  hostname: string
  timestamp: string
  serviceName: string
  applicationType: string
  user: {
    oid: string
  }
  target: {
    oppijaHenkiloOid: string
  }
  organizationOid: string
  operation: string
}
