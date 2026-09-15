export function truncateText(text: string, maxLength: number) {
  const characters = [...text]
  return characters.length > maxLength ? `${characters.slice(0, maxLength).join('')}…` : text
}
