package net.brainage.example.model;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class TestBookBackup {

    private int id;
    private int bookId;
    private String name;
    private int originPrice;
    private Date createdOn;

}
