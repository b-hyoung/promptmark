package local.promptmark.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class AssetDaoHybridScoreTest {

    @Test
    void keyword_score_zero_when_no_tokens() {
        double s = AssetDao.keywordScore(new String[0], "title", "summary", Collections.emptyList());
        assertThat(s).isEqualTo(0.0);
    }

    @Test
    void keyword_score_partial_match_returns_fraction() {
        double s = AssetDao.keywordScore(
            new String[]{"자기소개서", "첨삭", "운동"},
            "자기소개서 첨삭 프롬프트",
            "지원 동기와 강점을 매끄럽게 정리합니다",
            Arrays.asList("취업"));
        // 2 out of 3 tokens match
        assertThat(s).isCloseTo(2.0 / 3.0, offset(1e-9));
    }

    @Test
    void keyword_score_via_tag_match() {
        double s = AssetDao.keywordScore(
            new String[]{"디자인"},
            "Logo prompt", "make a logo", Arrays.asList("디자인", "이미지"));
        assertThat(s).isEqualTo(1.0);
    }

    @Test
    void keyword_score_case_insensitive() {
        double s = AssetDao.keywordScore(
            new String[]{"react"},
            "React Hook Cheatsheet", "Common patterns", Collections.emptyList());
        assertThat(s).isEqualTo(1.0);
    }

    @Test
    void cosine_similarity_orthogonal_is_zero() {
        float[] a = {1f, 0f, 0f};
        float[] b = {0f, 1f, 0f};
        assertThat(AssetDao.cosineSimilarity(a, b)).isCloseTo(0.0, offset(1e-9));
    }

    @Test
    void cosine_similarity_parallel_is_one() {
        float[] a = {0.3f, 0.4f, 0.5f};
        float[] b = {0.6f, 0.8f, 1.0f};
        assertThat(AssetDao.cosineSimilarity(a, b)).isCloseTo(1.0, offset(1e-6));
    }

    @Test
    void cosine_similarity_handles_null_or_empty() {
        assertThat(AssetDao.cosineSimilarity(null, new float[]{1f})).isEqualTo(0.0);
        assertThat(AssetDao.cosineSimilarity(new float[0], new float[]{1f})).isEqualTo(0.0);
    }

    @Test
    void cosine_similarity_zero_vector_returns_zero() {
        float[] a = {0f, 0f, 0f};
        float[] b = {1f, 0f, 0f};
        assertThat(AssetDao.cosineSimilarity(a, b)).isEqualTo(0.0);
    }

    @Test
    void clip_score_clamps_to_unit_interval() {
        assertThat(AssetDao.clipScore(-0.5)).isEqualTo(0.0);
        assertThat(AssetDao.clipScore(2.0)).isEqualTo(1.0);
        assertThat(AssetDao.clipScore(0.6)).isEqualTo(0.6);
        assertThat(AssetDao.clipScore(Double.NaN)).isEqualTo(0.0);
        assertThat(AssetDao.clipScore(Double.POSITIVE_INFINITY)).isEqualTo(0.0);
    }
}
