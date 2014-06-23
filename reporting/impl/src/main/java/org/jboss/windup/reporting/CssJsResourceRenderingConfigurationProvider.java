package org.jboss.windup.reporting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.windup.config.RulePhase;
import org.jboss.windup.config.WindupConfigurationProvider;
import org.jboss.windup.config.graphsearch.GraphSearchConditionBuilder;
import org.jboss.windup.config.operation.Iteration;
import org.jboss.windup.config.operation.ruleelement.AbstractIterationOperator;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.WindupConfigurationModel;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;

public class CssJsResourceRenderingConfigurationProvider extends WindupConfigurationProvider
{

    @Override
    public RulePhase getPhase()
    {
        return RulePhase.REPORTING;
    }

    @Override
    public Configuration getConfiguration(GraphContext context)
    {
        GraphSearchConditionBuilder configSearch = GraphSearchConditionBuilder.create("configuration").ofType(
                    WindupConfigurationModel.class);

        Configuration configuration = ConfigurationBuilder
                    .begin()
                    .addRule()
                    .when(configSearch)
                    .perform(
                                Iteration.over("configuration")
                                            .var("cfg")
                                            .perform(
                                                        new AbstractIterationOperator<WindupConfigurationModel>(
                                                                    WindupConfigurationModel.class, "cfg")
                                                        {
                                                            public void perform(
                                                                        org.jboss.windup.config.GraphRewrite event,
                                                                        org.ocpsoft.rewrite.context.EvaluationContext context,
                                                                        WindupConfigurationModel payload)
                                                            {
                                                                String outputPath = payload.getOutputPath();
                                                                copyCssResourcesToOutput(outputPath);
                                                            }
                                                        }
                                            ).endIteration()
                    );
        return configuration;
    }

    private void copyCssResourcesToOutput(String outputDir)
    {
        Path outputPath = Paths.get(outputDir, "resources");
        try
        {
            String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            File fpath = new File(path);
            if (fpath.isDirectory())
            {
                Path p = Paths.get(fpath.getAbsolutePath(), "reports/resources");
                recursePath(p, outputPath);
            }
            else
            {
                FileSystem fs = FileSystems.newFileSystem(new URI(path), null);
                Path p = fs.getPath("reports/resources");
                recursePath(p, outputPath);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception reading resource.", e);
        }
    }

    public void recursePath(final Path path, final Path resultPath) throws IOException
    {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                String relativePath = StringUtils.substringAfter(file.toString(), path.toString());
                relativePath = StringUtils.removeStart(relativePath, File.separator);
                Path resultFile = resultPath.resolve(relativePath);

                FileUtils.forceMkdir(resultFile.getParent().toFile());
                FileOutputStream fos = new FileOutputStream(resultFile.toFile());
                try
                {
                    Files.copy(file, fos);
                }
                finally
                {
                    IOUtils.closeQuietly(fos);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

}