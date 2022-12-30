package me.oxstone.googlenmtapplier.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.persistence.*;

@Entity(name = "lang")
@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class Language {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Nonnull
    private String name;
    @Nonnull
    private String code;
    @Nonnull
    private Boolean glossary;
}
