export const todayISODate = () => {
  const parts = new Intl.DateTimeFormat("fi-FI", {
    timeZone: "Europe/Helsinki",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date())
  const get = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((p) => p.type === type)!.value
  return `${get("year")}-${get("month")}-${get("day")}`
}
