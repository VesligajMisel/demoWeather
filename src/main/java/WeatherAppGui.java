import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.json.simple.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;

public class WeatherAppGui extends JFrame {
    private JSONObject weatherData;
    private DefaultCategoryDataset dataset;

    public WeatherAppGui(){
        super("Demo Weather app");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 650);
        setLocationRelativeTo(null);
        setLayout(null);
        setResizable(false);
        addGuiComponents();
        setVisible(true);
    }

    private void addGuiComponents(){
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel currentWeatherPanel = createCurrentWeatherPanel();
        tabbedPane.addTab("Current Weather", currentWeatherPanel);
        JPanel chartsPanel = createChartsPanel();
        tabbedPane.addTab("Daily Temperature", chartsPanel);
        tabbedPane.setBounds(0, 0, 800, 600);
        add(tabbedPane);
    }

    private JPanel createCurrentWeatherPanel() {
        JPanel currentWeatherPanel = new JPanel();
        currentWeatherPanel.setLayout(null);
        JTextField searchTextField = new JTextField();
        searchTextField.setBounds(15, 15, 351, 45);
        searchTextField.setFont(new Font("Dialog", Font.PLAIN, 24));
        currentWeatherPanel.add(searchTextField);
        JLabel weatherConditionImage = new JLabel(loadImage("cloudy.png"));
        weatherConditionImage.setBounds(0, 125, 450, 217);
        currentWeatherPanel.add(weatherConditionImage);
        JLabel temperatureText = new JLabel("10 C");
        temperatureText.setBounds(0, 350, 450, 54);
        temperatureText.setFont(new Font("Dialog", Font.BOLD, 48));
        temperatureText.setHorizontalAlignment(SwingConstants.CENTER);
        currentWeatherPanel.add(temperatureText);
        JLabel weatherConditionDesc = new JLabel("Cloudy");
        weatherConditionDesc.setBounds(0, 405, 450, 36);
        weatherConditionDesc.setFont(new Font("Dialog", Font.PLAIN, 32));
        weatherConditionDesc.setHorizontalAlignment(SwingConstants.CENTER);
        currentWeatherPanel.add(weatherConditionDesc);
        JLabel humidityImage = new JLabel(loadImage("humidity.png"));
        humidityImage.setBounds(15, 500, 74, 66);
        currentWeatherPanel.add(humidityImage);
        JLabel humidityText = new JLabel("<html><b>Humidity</b> 100%</html>");
        humidityText.setBounds(90, 500, 85, 55);
        humidityText.setFont(new Font("Dialog", Font.PLAIN, 16));
        currentWeatherPanel.add(humidityText);
        JLabel windSpeedImage = new JLabel(loadImage("windspeed.png"));
        windSpeedImage.setBounds(220, 500, 74, 66);
        currentWeatherPanel.add(windSpeedImage);
        JLabel windSpeedText = new JLabel("<html><b>Windspeed</b> 15km/h</html>");
        windSpeedText.setBounds(310, 500, 85, 55);
        windSpeedText.setFont(new Font("Dialog", Font.PLAIN, 16));
        currentWeatherPanel.add(windSpeedText);
        JButton searchButton = new JButton(loadImage("search.png"));
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.setBounds(375, 13, 47, 45);
        searchButton.addActionListener(e -> {
            String userInput = searchTextField.getText();
            if(userInput.replaceAll("\\s", "").length() <= 0){
                return;
            }
            weatherData = WeatherApp.getWeatherData(userInput);
            if (weatherData != null) {
                String weatherCondition = (String) weatherData.get("weather_condition");
                switch (weatherCondition) {
                    case "Clear":
                        weatherConditionImage.setIcon(loadImage("clear.png"));
                        break;
                    case "Cloudy":
                        weatherConditionImage.setIcon(loadImage("cloudy.png"));
                        break;
                    case "Rain":
                        weatherConditionImage.setIcon(loadImage("rain.png"));
                        break;
                    case "Snow":
                        weatherConditionImage.setIcon(loadImage("snow.pngImage"));
                        break;
                }
                double temperature = (double) weatherData.get("temperature");
                temperatureText.setText(temperature + " C");
                weatherConditionDesc.setText(weatherCondition);
                long humidity = (long) weatherData.get("humidity");
                humidityText.setText("<html><b>Humidity</b> " + humidity + "%</html>");
                double windspeed = (double) weatherData.get("windspeed");
                windSpeedText.setText("<html><b>Windspeed</b> " + windspeed + "km/h</html>");
            }

            assert weatherData != null;
            java.util.List<WeatherApp.WeatherHour> hourlyData = WeatherApp.processHourlyData(weatherData);
            updateChartData(hourlyData);
        });
        currentWeatherPanel.add(searchButton);
        return currentWeatherPanel;
    }

    private JPanel createChartsPanel() {
        JPanel chartsPanel = new JPanel(new BorderLayout());
        dataset = new DefaultCategoryDataset();
        dataset.addValue(0, "Temperature", "01:00");
        JFreeChart chart = createTemperatureChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartsPanel.add(chartPanel, BorderLayout.CENTER);
        return chartsPanel;
    }

    private JFreeChart createTemperatureChart(DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createLineChart(
                "Temperature Variation",
                "Time",
                "Temperature (°C)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));

        return chart;
    }

    private void updateChartData(java.util.List<WeatherApp.WeatherHour> weatherHours) {
        dataset.clear();
        for (WeatherApp.WeatherHour hour : weatherHours) {
            Instant dateTime = hour.getDateTime();
            double temperature = hour.getTemperature();
            String hourStr = formatHour(dateTime.atZone(ZoneOffset.UTC).toLocalDateTime().getHour());
            dataset.addValue(temperature, "Temperature", hourStr + ":00");
        }
    }


    private String formatHour(int hour) {
        return String.format("%02d", hour);
    }

    private ImageIcon loadImage(String filename) {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/" + filename);
            if (inputStream != null) {
                return new ImageIcon(ImageIO.read(inputStream));
            } else {
                System.err.println("Unable to find image: " + filename);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherAppGui::new);
    }
}

