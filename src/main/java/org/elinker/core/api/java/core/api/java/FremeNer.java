package org.elinker.core.api.java.core.api.java;


import org.elinker.core.api.process.Datasets;

import java.util.List;

/**
 * Created by nilesh on 12/10/15.
 */
public class FremeNer {
    private Config config = null;
    private org.elinker.core.api.scala.FremeNer fremeNer = null;

    public FremeNer(Config config) {
        this.config = config;
        org.elinker.core.api.scala.Config scalaConfig = config.getScalaConfig();
        fremeNer = new org.elinker.core.api.scala.FremeNer(scalaConfig);
    }

    public String spot(String text, String language, String outputFormat, String rdfPrefix) {
        return fremeNer.spot(text, language, outputFormat, rdfPrefix);
    }

    public String spotClassify(String text, String language, String outputFormat, String rdfPrefix) {
        return fremeNer.spotClassify(text, language, outputFormat, rdfPrefix);
    }

    public String spotLink(String text, String language, String dataset, String outputFormat, String rdfPrefix, Integer numLinks) {
        return fremeNer.spotLink(text, language, dataset, outputFormat, rdfPrefix, numLinks);
    }

    public String spotLinkClassify(String text, String language, String dataset, String outputFormat, String rdfPrefix, Integer numLinks) {
        return fremeNer.spotLinkClassify(text, language, dataset, outputFormat, rdfPrefix, numLinks);
    }

    public Datasets.Dataset addDataset(String name, org.elinker.core.api.scala.FremeNer.InputType dataset, String description, String format, String language, String[] properties) {
        return fremeNer.addDataset(name, dataset, description, format, language, properties);
    }

    public Datasets.Dataset updateDataset(String name, org.elinker.core.api.scala.FremeNer.InputType dataset, String description, String format, String language, String[] properties) {
        return fremeNer.updateDataset(name, dataset, description, format, language, properties);
    }

    public void deleteDataset(String name) {
        fremeNer.deleteDataset(name);
    }

    public Datasets.Dataset getDataset(String name) {
        return fremeNer.getDataset(name);
    }

    public Datasets.Dataset[] getAllDatasets() {
        return fremeNer.getAllDatasets();
    }
}