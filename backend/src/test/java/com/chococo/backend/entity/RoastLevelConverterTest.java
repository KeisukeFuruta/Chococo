package com.chococo.backend.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

// database-design.md 2.2節のシードデータはroast_levelを日本語文字列（ブロンド/ミディアム/ダーク）で保持するため、
// @Enumerated(STRING)ではなくAttributeConverterで変換している。この対応関係が崩れるとFlywayのシードデータと
// エンティティの間で静かにデータ不整合が起きるため、変換の往復を明示的に固定する
class RoastLevelConverterTest {

    private final RoastLevelConverter converter = new RoastLevelConverter();

    @Test
    void convertsEachRoastLevelToItsJapaneseDbValue() {
        assertThat(converter.convertToDatabaseColumn(RoastLevel.BLONDE)).isEqualTo("ブロンド");
        assertThat(converter.convertToDatabaseColumn(RoastLevel.MEDIUM)).isEqualTo("ミディアム");
        assertThat(converter.convertToDatabaseColumn(RoastLevel.DARK)).isEqualTo("ダーク");
    }

    @Test
    void convertsEachJapaneseDbValueBackToTheSameRoastLevel() {
        for (RoastLevel level : RoastLevel.values()) {
            String dbValue = converter.convertToDatabaseColumn(level);
            assertThat(converter.convertToEntityAttribute(dbValue)).isEqualTo(level);
        }
    }

    @Test
    void convertsNullInBothDirections() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void rejectsUnknownDbValueInsteadOfSilentlyReturningNull() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("ライト"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ライト");
    }
}
