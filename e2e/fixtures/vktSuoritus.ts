export const create = async () => {
  const response = await fetch(
    "http://localhost:8080/dev/mockdata/vkt/suoritus/1000",
  )
  return await response.json()
}
