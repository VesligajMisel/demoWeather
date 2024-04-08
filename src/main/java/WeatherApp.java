import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WeatherApp {
    /**
     * retrieve weather data from API for given location - this will fetch the latest weather
     * data from the external API and return it.
     * @param locationName
     * @return JSONObject
     */
    public static JSONObject getWeatherData(String locationName){
        JSONArray locationData = getLocationData(locationName);

        assert locationData != null;
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relativehumidity_2m,weathercode,windspeed_10m&timezone=Europe/Ljubljana";

        try{
            HttpURLConnection conn = fetchApiResponse(urlString);

            assert conn != null;
            if(conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }

            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while(scanner.hasNext()){
                resultJson.append(scanner.nextLine());
            }

            scanner.close();
            conn.disconnect();

            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            JSONArray weathercode = (JSONArray) hourly.get("weathercode");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            JSONArray relativeHumidity = (JSONArray) hourly.get("relativehumidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            JSONArray windspeedData = (JSONArray) hourly.get("windspeed_10m");
            double windspeed = (double) windspeedData.get(index);

            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);
            weatherData.put("hourly", hourly);

            return weatherData;
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public static List<WeatherHour> processHourlyData(JSONObject response) {
        List<WeatherHour> weatherHours = new ArrayList<>();

        JSONObject hourlyData = (JSONObject) response.get("hourly");
        JSONArray timeArray = (JSONArray) hourlyData.get("time");
        JSONArray temperatureArray = (JSONArray) hourlyData.get("temperature_2m");
        JSONArray humidityArray = (JSONArray) hourlyData.get("relativehumidity_2m");
        JSONArray windSpeedArray = (JSONArray) hourlyData.get("windspeed_10m");

        for (int i = 0; i < timeArray.size(); i++) {
            String dateTimeString = (String) timeArray.get(i);
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeString);

            Number temperatureNumber = (Number) temperatureArray.get(i);
            double temperature = temperatureNumber.doubleValue();

            Number humidityNumber = (Number) humidityArray.get(i);
            double humidity = humidityNumber.doubleValue();

            Number windSpeedNumber = (Number) windSpeedArray.get(i);
            double windSpeed = windSpeedNumber.doubleValue();

            WeatherHour weatherHour = new WeatherHour(dateTime.atZone(ZoneOffset.UTC).toInstant(), temperature, humidity, windSpeed);
            weatherHours.add(weatherHour);
        }

        return weatherHours;
    }

    /**
     * Retrieve geographic coordinates for given location name
     * @param locationName
     * @return JSONArray
     */
    public static JSONArray getLocationData(String locationName){
        locationName = locationName.replaceAll(" ", "+");

        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try{
            HttpURLConnection conn = fetchApiResponse(urlString);
            assert conn != null;
            if(conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }else{
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());

                while(scanner.hasNext()){
                    resultJson.append(scanner.nextLine());
                }

                scanner.close();
                conn.disconnect();

                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                return (JSONArray) resultsJsonObj.get("results");
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString){
        try{
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            return conn;
        }catch(IOException e){
            e.printStackTrace();
        }

        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList){
        String currentTime = getCurrentTime();

        for(int i = 0; i < timeList.size(); i++){
            String time = (String) timeList.get(i);
            if(time.equalsIgnoreCase(currentTime)){
                return i;
            }
        }

        return 0;
    }

    private static String getCurrentTime(){
        LocalDateTime currentDateTime = LocalDateTime.now();

        // format date to be 2023-09-02T00:00
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");
        return currentDateTime.format(formatter);
    }

    /**
     * Convert the weather code to something more readable.
     * @param weatherCode
     * @return
     */
    private static String convertWeatherCode(long weatherCode){
        String weatherCondition = "";
        if(weatherCode == 0L){
            // clear
            weatherCondition = "Clear";
        }else if(weatherCode > 0L && weatherCode <= 3L){
            // cloudy
            weatherCondition = "Cloudy";
        }else if((weatherCode >= 51L && weatherCode <= 67L)
                    || (weatherCode >= 80L && weatherCode <= 99L)){
            // rain
            weatherCondition = "Rain";
        }else if(weatherCode >= 71L && weatherCode <= 77L){
            // snow
            weatherCondition = "Snow";
        }

        return weatherCondition;
    }
    static class WeatherHour {
        private final Instant dateTime;
        private final double temperature;
        private final double humidity;
        private final double windSpeed;

        public WeatherHour(Instant dateTime, double temperature, double humidity, double windSpeed) {
            this.dateTime = dateTime;
            this.temperature = temperature;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
        }

        // Getter methods
        public Instant getDateTime() {
            return dateTime;
        }

        public double getTemperature() {
            return temperature;
        }

        public double getHumidity() {
            return humidity;
        }

        public double getWindSpeed() {
            return windSpeed;
        }
    }
}








