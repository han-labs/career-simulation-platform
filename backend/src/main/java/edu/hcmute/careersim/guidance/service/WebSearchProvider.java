package edu.hcmute.careersim.guidance.service;

import java.util.List;

/** Public learning discovery only; implementations never receive private evidence. */
public interface WebSearchProvider {
    Result search(String publicQuery);

    record Source(String referenceId, String title, String url, String excerpt) {}

    record Result(
            String status, String retrievedAt, List<Source> sources, String searchSuggestionsHtml) {
        public Result {
            sources = List.copyOf(sources);
        }

        public static Result unavailable(String status) {
            return new Result(status, null, List.of(), null);
        }
    }
}
