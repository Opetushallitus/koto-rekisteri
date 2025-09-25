type CloudWatchData = {
  logEvents: {
    message: string // JSON containing KituLogEvent
  }[]
}

type KituLogEvent = {
  message: string // JSON containing AuditLogEntry
}

type AuditLogEntry = {
  version: number
  logSeq: number
  bootTime: string
  type: string
  environment: string
  hostname: string // "hostname":"not set",
  timestamp: string
  serviceName: string
  applicationType: string
  user: {
    oid: string // {}
  }
  target: {
    oppijaHenkiloOid: string // {}
  }
  organizationOid: string
  operation: string
}



