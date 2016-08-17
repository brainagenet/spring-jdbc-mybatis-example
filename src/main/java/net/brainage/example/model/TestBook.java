package net.brainage.example.model;

import lombok.*;

import java.util.Date;

/**
 * Created by ms29.seo on 2016-08-17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class TestBook {

    private int id;
    private String name;
    private int originPrice;
    private Date createdOn;

}
