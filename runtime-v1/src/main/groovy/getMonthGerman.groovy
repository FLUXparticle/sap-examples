import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

def String getMonat(LocalDate date = LocalDate.now()) {
    return date.format(DateTimeFormatter.ofPattern("MMMM", Locale.GERMAN))
}
