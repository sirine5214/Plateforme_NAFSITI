package tn.esprit.pi.nefsiti.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** À poser avec @Convert sur les colonnes sensibles : chiffrées en base, en clair dans l'application. */
@Converter
public class ChiffrementConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribut) {
        return Chiffrement.chiffrer(attribut);
    }

    @Override
    public String convertToEntityAttribute(String colonne) {
        return Chiffrement.dechiffrer(colonne);
    }
}
