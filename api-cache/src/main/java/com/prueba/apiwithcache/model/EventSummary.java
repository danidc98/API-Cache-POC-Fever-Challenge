package com.prueba.apiwithcache.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**Definimos el esquema del objeto EventSummary y especificamos las anotaciones de la librería Jackson
 * para decirle que debe ignorar el campo endTimestamp y debe cambiar el nombre de los campos escritos en camel case
 * a palabras separadas por "_".
 *
 */
public class EventSummary implements Serializable {

    private String id;
    private String title;

    private Long endTimestamp;

    @JsonIgnore
    public Long getEndTimestamp() {
        return endTimestamp;
    }
    @JsonProperty("start_date")
    private String startDate;
    @JsonProperty("start_time")

    private String startTime;
    @JsonProperty("end_date")

    private String endDate;
    @JsonProperty("end_time")

    private String endTime;

    //Un número de simple precisión es suficiente para un precio
    @JsonProperty("min_price")

    private float minPrice;
    @JsonProperty("max_price")

    private float maxPrice;

    public EventSummary() {
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getEndTime() {
        return endTime;
    }

    public float getMinPrice() {
        return minPrice;
    }

    public float getMaxPrice() {
        return maxPrice;
    }

    /**
     * Con el patrón builder conseguimos un código más legible y más fácil de emplear
     * por otro desarrollador, pues no necesitará recordar el orden de los parámetros del constructor.
     * Empleamos los campos que no aparecen como Nullable en la docu:
     * https://app.swaggerhub.com/apis-docs/luis-pintado-feverup/backend-test/1.0.0#/EventList,
     * para el constructor Builder y los nullables se establecerán con withBlablabla().
     */
    public static class Builder {
        protected String id;
        protected String title;
        protected String startDate;
        protected String startTime;
        protected String endDate;
        protected String endTime;

        //Un número de simple precisión es suficiente para un precio
        protected float minPrice;
        protected float maxPrice;
        protected Long endTimestamp;

        public Builder(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public Builder withStartDate(String startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withEndTimestamp(Long endTimestamp) {
            this.endTimestamp = endTimestamp;
            return this;
        }

        public Builder withStartTime(String startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withEndDate(String endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder withEndTime(String endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder withMinPrice(float minPrice) {
            this.minPrice = minPrice;
            return this;
        }

        public Builder withMaxPrice(float maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }

        public EventSummary build() {
            return new EventSummary(this);
        }
    }

    public EventSummary(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.startDate = builder.startDate;
        this.startTime = builder.startTime;
        this.endDate = builder.endDate;
        this.endTime = builder.endTime;
        this.minPrice = builder.minPrice;
        this.maxPrice = builder.maxPrice;
        this.endTimestamp=builder.endTimestamp;
    }
}
