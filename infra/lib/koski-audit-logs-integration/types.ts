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
  type: string
  // TODO: environment
  bootTime: string
  hostname: string
  timestamp: string
  serviceName: string
  applicationType: string
  user: {
    oid: string
    ip: string
    session: string
    userAgent: string
  }
  operation: "KielitestiSuoritusViewed"
  target: { oppijaHenkiloOid: "1.2.246.562.198.88975028874" }
  // TODO: organizationOid
  // TODO: operation
  changes: []
}
