package com.example.weatherwidgets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.weatherwidgets.databinding.ActivityMainBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.net.UnknownHostException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.fahrenheit = binding.rbF.isChecked
        binding.showWind = binding.cbWind.isChecked

        binding.rgUnits.setOnCheckedChangeListener { _, checkedId ->
            binding.fahrenheit = (checkedId == R.id.rb_f)
        }

        binding.cbWind.setOnCheckedChangeListener { _, isChecked ->
            binding.showWind = isChecked
        }

        binding.spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                refresh()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun onRefreshClick(v: View) = refresh()

    private fun refresh() {
        val city = binding.spinnerCity.selectedItem.toString()
        lifecycleScope.launch {
            try {
                val (weather, icon) = loadWeather(city)
                binding.error = null
                binding.weather = weather
                binding.ivIcon.setImageBitmap(icon)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                binding.weather = null
                binding.error = errorMessage(e)
            }
        }
    }

    private suspend fun loadWeather(city: String): Pair<Weather, Bitmap?> = withContext(Dispatchers.IO) {
        val key = getString(R.string.owm_api_key)
        val url = "https://api.openweathermap.org/data/2.5/weather" +
                "?q=$city&appid=$key&units=metric&lang=ru"
        val json = URL(url).readText()
        if (json.isBlank()) throw IllegalStateException("empty")
        val weather = parseWeather(json)

        val icon = try {
            URL("https://openweathermap.org/img/wn/${weather.icon}@2x.png")
                .openStream().use { BitmapFactory.decodeStream(it) }
        } catch (e: IOException) {
            null
        }
        Pair(weather, icon)
    }

    private fun parseWeather(json: String): Weather {
        val o = JSONObject(json)
        val main = o.getJSONObject("main")
        val wind = o.getJSONObject("wind")
        val w0 = o.getJSONArray("weather").getJSONObject(0)
        return Weather(
            city = o.getString("name"),
            description = w0.getString("description"),
            icon = w0.getString("icon"),
            temp = main.getDouble("temp"),
            feelsLike = main.getDouble("feels_like"),
            humidity = main.getInt("humidity"),
            pressure = main.getInt("pressure"),
            windSpeed = wind.getDouble("speed"),
            windDeg = wind.optDouble("deg", 0.0).toFloat(),
            clouds = o.getJSONObject("clouds").getInt("all")
        )
    }

    private fun errorMessage(e: Exception): String = when (e) {
        is UnknownHostException -> "Нет подключения к интернету"
        is FileNotFoundException -> "Город не найден"
        is IOException ->
            if (e.message?.contains("401") == true || e.message?.contains("authentication") == true)
                "Неверный или ещё не активированный API-ключ"
            else "Ошибка сети: ${e.message}"
        is JSONException, is IllegalStateException -> "Сервер вернул некорректный ответ"
        else -> "Ошибка: ${e.message}"
    }
}