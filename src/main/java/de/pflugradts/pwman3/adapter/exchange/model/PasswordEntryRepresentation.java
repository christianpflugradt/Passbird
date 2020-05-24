package de.pflugradts.pwman3.adapter.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PasswordEntryRepresentation {
    private String key;
    private String password;
}
