export const getEnv = (name: string) => {
  const value = process.env[name]
  if (value === undefined) {
    throw new Error(`Environment variable ${name} required`)
  }
  return value
}
