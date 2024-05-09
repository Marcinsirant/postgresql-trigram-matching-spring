package pl.sirant.tm.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;

import java.util.Arrays;

public class SimilarityMatchUtil {

  private SimilarityMatchUtil() {
  }

  public static Order similarityOrder(CriteriaBuilder cb, String queryParam, Path<String>... objectPaths) {
    return Arrays.stream(objectPaths).map(e -> cb.function("similarity", Float.class, e, cb.literal(queryParam)))
        .reduce(cb::sum)
        .map(cb::desc)
        .orElse(null);
  }

}
