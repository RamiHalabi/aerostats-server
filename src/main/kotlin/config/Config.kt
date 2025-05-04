package config

object Config {
    val SUPABASE_URL: String = EnvLoader.get("SUPABASE_URL") ?: System.getenv("SUPABASE_URL") ?: error("SUPABASE_URL is not set")
    val SUPABASE_KEY: String = EnvLoader.get("SUPABASE_KEY") ?: System.getenv("SUPABASE_KEY") ?: error("SUPABASE_KEY is not set")
    val FR24_API_KEY: String = EnvLoader.get("FR24_API_KEY") ?: System.getenv("FR24_API_KEY") ?: error("FR24_API_KEY is not set")
    val FR24_URL: String = EnvLoader.get("FR24_URL") ?: System.getenv("FR24_URL") ?: error("FR24_URL is not set")
}