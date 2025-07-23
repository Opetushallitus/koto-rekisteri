export const create = async (baseUrl: string) => {
  const response = await fetch(
    new URL("dev/mockdata/vkt/suoritus/1000", baseUrl),
  )
  return await response.json()
}
