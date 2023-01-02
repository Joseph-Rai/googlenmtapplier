package me.oxstone.googlenmtapplier.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import me.oxstone.googlenmtapplier.data.Language;

public interface LanguageRepository extends JpaRepository<Language, Long> {
    Optional<Language> findByCode(String code);

    Optional<Language> findByName(String name);
}
