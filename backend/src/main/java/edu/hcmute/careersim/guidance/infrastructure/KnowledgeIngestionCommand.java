package edu.hcmute.careersim.guidance.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.guidance.dao.KnowledgeDao;
import edu.hcmute.careersim.guidance.service.KnowledgeIngestionService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Opt-in internal ingestion, never an endpoint or model tool. */
@Component
@ConditionalOnProperty(name = "app.knowledge.ingest-file")
public class KnowledgeIngestionCommand implements ApplicationRunner {
    private final String file;
    private final ObjectMapper mapper;
    private final KnowledgeIngestionService ingestion;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ConfigurableApplicationContext applicationContext;

    @Value("${app.knowledge.exit-after-ingest:false}")
    private boolean exitAfterIngest;

    public KnowledgeIngestionCommand(
            @Value("${app.knowledge.ingest-file}") String file,
            ObjectMapper mapper,
            KnowledgeIngestionService ingestion) {
        this.file = file;
        this.mapper = mapper;
        this.ingestion = ingestion;
    }

    public void run(ApplicationArguments args) throws Exception {
        Path path = Path.of(file);
        if (Files.size(path) > 2_000_000) throw new IllegalArgumentException("Manifest too large");
        List<KnowledgeDao.Document> docs =
                mapper.readValue(Files.readString(path), new TypeReference<>() {});
        if (docs.size() > 100) throw new IllegalArgumentException("Too many documents");
        for (var doc : docs) ingestion.publish(doc);
        if (exitAfterIngest) org.springframework.boot.SpringApplication.exit(applicationContext);
    }
}
