#!/usr/bin/env node
//
// Tulostaa CDK-appin sisältämät pinot muodossa `tili<TAB>alue<TAB>pinonimi`.
//
// Lähteenä on synteesin tuottama cloud assembly eikä `cdk list`, koska `cdk
// list` antaa vain konstruktipolun (`Prod/Alarms`) — vertailu deployattuihin
// pinoihin tarvitsee oikean pinonimen (`Prod-Alarms`) ja tili/aluetiedon.

import { readdirSync, readFileSync } from "node:fs"
import { join } from "node:path"

const assemblyDir = process.argv[2] ?? "cdk.out"

const readManifest = (dir) =>
  JSON.parse(readFileSync(join(dir, "manifest.json"), "utf8"))

const collect = (dir, out) => {
  for (const [id, artifact] of Object.entries(
    readManifest(dir).artifacts ?? {},
  )) {
    if (artifact.type === "cdk:cloud-assembly") {
      collect(join(dir, artifact.properties.directoryName), out)
    } else if (artifact.type === "aws:cloudformation:stack") {
      const [, account, region] =
        /^aws:\/\/([^/]+)\/([^/]+)$/.exec(artifact.environment) ?? []
      if (!account || account === "unknown-account") {
        throw new Error(`Pinolla ${id} ei ole kiinnitettyä tiliä ja aluetta`)
      }
      out.push([account, region, artifact.properties?.stackName ?? id])
    }
  }
  return out
}

readdirSync(assemblyDir) // heittää selkeän virheen jos synteesiä ei ole ajettu
for (const row of collect(assemblyDir, [])) {
  console.log(row.join("\t"))
}
