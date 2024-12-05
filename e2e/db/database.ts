import * as pg from "pg"
import * as assert from "node:assert"
import { SQLStatement } from "sql-template-strings"
import config from "../config"

const pool = new pg.Pool({
  connectionString: config.database.connectionString,
  ssl: false,
})

const createQuery =
  (client: pg.PoolClient) =>
  async <T extends {}>(statement: string | SQLStatement) => {
    const { rows } = await client.query(statement)
    return rows as T[]
  }

const createQueryOne =
  (client: pg.PoolClient) =>
  async <T extends {}>(statement: string | SQLStatement) => {
    const { rows } = await client.query(statement)

    assert(
      rows.length === 1,
      `'queryOne' should always return one row, got ${rows.length}`,
    )

    return rows[0] as T
  }

const createQueryOneOrNone =
  (client: pg.PoolClient) =>
  async <T extends {}>(statement: string | SQLStatement): Promise<T | null> => {
    const { rows } = await client.query(statement)

    assert(
      rows.length === 1 || rows.length === 0,
      `'queryOneOrNone' should always return one or zero rows, got ${rows.length}`,
    )

    return (rows[0] as T | null | undefined) ?? null
  }

const query: ReturnType<typeof createQuery> = async (...args) => {
  const client = await pool.connect()
  try {
    return createQuery(client)(...args)
  } finally {
    client.release()
  }
}

const queryOne: ReturnType<typeof createQueryOne> = async (...args) => {
  const client = await pool.connect()
  try {
    return createQueryOne(client)(...args)
  } finally {
    client.release()
  }
}

const queryOneOrNone: ReturnType<typeof createQueryOneOrNone> = async (
  ...args
) => {
  const client = await pool.connect()
  try {
    return createQueryOneOrNone(client)(...args)
  } finally {
    client.release()
  }
}

export interface TransactionClient {
  query: typeof query
  queryOne: typeof queryOne
  queryOneOrNone: typeof queryOneOrNone
}

const transact = async <T>(
  transaction: (client: TransactionClient) => Promise<T>,
) => {
  const client = await pool.connect()
  try {
    await client.query("BEGIN")
    const res = await transaction({
      query: createQuery(client),
      queryOne: createQueryOne(client),
      queryOneOrNone: createQueryOneOrNone(client),
    })
    await client.query("COMMIT")
    return res
  } catch (err) {
    await client.query("ROLLBACK")
    throw err
  } finally {
    client.release()
  }
}

export default {
  dbClient: {
    pool,
    query,
    queryOne,
    queryOneOrNone,
    transact,
  },
}
