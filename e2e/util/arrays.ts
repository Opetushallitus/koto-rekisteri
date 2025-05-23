export const enumerate = <T>(arr: readonly T[]): [T, number][] =>
  arr.map((v, i) => [v, i])
