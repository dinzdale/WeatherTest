package model;

import java.util.List;

/**
 * Created by gjacobs on 10/31/15.
 */
public class Forecast {
    City city;
    String cod;
    float message;
    int cnt;
    List<ForecastDetails> list;

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public String getCod() {
        return cod;
    }

    public void setCod(String cod) {
        this.cod = cod;
    }

    public float getMessage() {
        return message;
    }

    public void setMessage(float message) {
        this.message = message;
    }

    public int getCnt() {
        return cnt;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public List<ForecastDetails> getList() {
        return list;
    }

    public void setList(List<ForecastDetails> list) {
        this.list = list;
    }
}
