package me.oxstone.googlenmtapplier.repository;

import me.oxstone.googlenmtapplier.data.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LanguageRepository extends JpaRepository<Language, Long> {
    Optional<Language> findByCode(String code);
    Optional<Language> findByName(String name);
}
