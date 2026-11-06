package by.grinich.hscontrol

/**
 * Настройки подключения к Supabase.
 *
 * ⚠️ ВАЖНО: замените значения на свои из панели Supabase:
 * Project Settings → API → Project URL / anon public key
 */
object SupabaseConfig {
    // Пример: https://abcdefghijklm.supabase.co
    const val URL = "https://xkdlqdycbwmjwxxkalfn.supabase.co"

    // Очень длинный ключ (начинается на eyJ...)
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhrZGxxZHljYndtand4eGthbGZuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk0NjE1MzIsImV4cCI6MjEwNTAzNzUzMn0.nE8_S_R3_ozsF45EX1vTuXz7UGv7-H4PK9CpDl-n4mg"
}