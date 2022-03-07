/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package decisionmakertool.owl;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.clarkparsia.pellet.owlapi.PelletReasonerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import decisionmakertool.service.BabelnetService;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.impl.blackbox.checker.InconsistentOntologyExplanationGeneratorFactory;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import uk.ac.manchester.cs.jfact.JFactFactory;
import javax.faces.context.FacesContext;

public class OntologyUtil<BabelnetService> {

    private OWLOntology ontology;
    private OWLOntologyManager manager;
    private OWLReasonerFactory factory = null;
    private OWLReasoner reasoner;
    private OWLDataFactory dataFactory = null;
    private BabelnetService babelnetService;

    public OntologyUtil(BabelnetService babelnetService) {
        this.babelnetService = babelnetService;
    }

    public OntologyUtil(String path) {
        try {
            if(!path.isEmpty()){
                File file = new File(path);
                manager = OWLManager.createOWLOntologyManager();
                ontology = getManager().loadOntologyFromOntologyDocument(file);
                factory = new JFactFactory();
                reasoner = this.factory.createReasoner(ontology);
                dataFactory = getManager().getOWLDataFactory();
            }
        } catch (OWLOntologyCreationException ex) {
            Logger.getLogger(OntologyUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public OWLOntology getOntology(String path) {
        try {
            if(!path.isEmpty()){
                File file = new File(path);
                manager = OWLManager.createOWLOntologyManager();
                ontology = getManager().loadOntologyFromOntologyDocument(file);
                factory = new JFactFactory();
                reasoner = this.factory.createReasoner(ontology);
                dataFactory = getManager().getOWLDataFactory();
            }
        } catch (OWLOntologyCreationException ex) {
            Logger.getLogger(OntologyUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ontology;
    }

    public String validationConsistency(String path) {
        String answer = "";
        File file = new File(path);

        try {
            manager = OWLManager.createOWLOntologyManager();
            ontology = getManager().loadOntologyFromOntologyDocument(file);
            manager = ontology.getOWLOntologyManager();
            dataFactory = getManager().getOWLDataFactory();
            factory = new JFactFactory();
            reasoner = factory.createReasoner(ontology);

            if (reasoner.isConsistent()) {
                int unsatisfiableClasses = 0;
                unsatisfiableClasses = reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().size();
                if (unsatisfiableClasses > 0) {
                    answer = "Merged ontology FAILED satisfiability test. Unsatisfiable classes detected: "
                            + reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().size();
                }
                answer = "Merged ontology PASSED the consistency test";
            }
            else
            {
                answer = "Merged ontology FAILED the consistency test, please review the Axioms or debug using Protege";
                answer += getAnswerExplanations(answer);
            }
        } catch (OWLOntologyCreationException | InconsistentOntologyException ex) {
            Logger.getLogger(OntologyUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }

    private String getAnswerExplanations(String answer) {
        ExplanationGenerator<OWLAxiom> explainInconsistency = new InconsistentOntologyExplanationGeneratorFactory(factory,
                1000L).createExplanationGenerator(ontology);
        // Ask for an explanation of `Thing subclass of Nothing` - this axiom is entailed in any inconsistent ontology
        Set<Explanation<OWLAxiom>> explanations = explainInconsistency.getExplanations(dataFactory.getOWLSubClassOfAxiom(dataFactory
                .getOWLThing(), dataFactory.getOWLNothing()));
        int cont = 1;

        for (Explanation<OWLAxiom> e : explanations) {
            answer += "Explain " + cont + "\n";
            answer += "Axioms causing the inconsistency:\n";

            for (OWLAxiom axiom : e.getAxioms()) {
                answer += "Axiom: " + axiom.getSignature() + "\n";
            }

            answer += "------------------" + "\n";
            cont++;
        }
        return answer;
    }

    public void getPath(){

    }

    public List<String> removeAxioms(String valueURI, String path) throws OWLOntologyStorageException {
        System.out.println("Element:" + valueURI);
        List<String> listChanges = new ArrayList<>();
        Set < OWLAxiom > axiomsToRemove =  new  HashSet <> ();
        for ( OWLAxiom axiom : ontology.getAxioms ()) {
            if (axiom.getSignature().toString().contains(valueURI)) {
                axiomsToRemove.add(axiom);
                listChanges.add("Axiom to remove:" + axiom);
                System.out.println( " para eliminar de "  + ontology.getOntologyID ().getOntologyIRI () +  " : "  + axiom);
            }
        }
         getManager().removeAxioms (ontology, axiomsToRemove);
         saveOntology(path);
         return listChanges;
    }

    public void saveOntology(String path) throws OWLOntologyStorageException {
        //String realpath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("/resources");
        File fileformated = new File(path);
        //Save the ontology in a different format
        OWLOntologyFormat format = getManager().getOntologyFormat(ontology);
        RDFXMLOntologyFormat owlxmlFormat = new RDFXMLOntologyFormat();
        if (format.isPrefixOWLOntologyFormat()) {
            owlxmlFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
        }
        getManager().saveOntology(ontology, owlxmlFormat, IRI.create(fileformated.toURI()));
    }

    public  Set<OWLClass> getClasses() {
        Set<OWLClass> classes = ontology.getClassesInSignature();
        return classes;
    }

    public List<String> equalsClasses(OWLOntology ontologyBase,OWLOntology ontologyAutomatic){
        Set<OWLClass> classesBase = ontology.getClassesInSignature();
        Set<OWLClass> classesAutomatic = ontology.getClassesInSignature();
        List<String> baseClasses = getListNameClasses(classesBase);
        List<String> automaticClasses = getListNameClasses(classesAutomatic);
        List<String> equalsClasses = baseClasses.stream()
                .filter(element -> automaticClasses.contains(element))
                .collect(Collectors.toList());
        return equalsClasses;
    }

    public List<String> getListNameClasses(Set<OWLClass> classes){
        List<String> listClasses = new ArrayList<>();
        for(OWLClass clazz: classes){
            String nameClassB = clazz.getIRI().getShortForm();
            listClasses.add(nameClassB);
        }
        return listClasses;
    }

    public List<String> differentClasses(OWLOntology ontologyBase,OWLOntology ontologyAutomatic){
        Set<OWLClass> classesBase = ontology.getClassesInSignature();
        Set<OWLClass> classesAutomatic = ontology.getClassesInSignature();
        List<String> baseClasses = getListNameClasses(classesBase);
        List<String> automaticClasses = getListNameClasses(classesAutomatic);
        List<String> differences = baseClasses.stream()
                .filter(element -> !automaticClasses.equals(element))
                .collect(Collectors.toList());

        return differences;
    }

    public List<String> getBabelnetExplanationClasses(OWLOntology ontologyAutomatic){
        Set<OWLClass> classesAutomatic = ontology.getClassesInSignature();
        List<String> result = new ArrayList<>();
        for(OWLClass classAutomatic: classesAutomatic){
            String nameClassA = classAutomatic.getIRI().getShortForm();
            String pos = babelnetService.getPOS(nameClassA);
            if(!pos.equals("NOUN")){
                result.add(nameClassA + " is a" + pos);
            }
        }
        return result;
    }

    public  Set<OWLClass> getUniqueClasses() {
        Set<OWLClass> classes = ontology.getClassesInSignature();
        Set<OWLClass>  uniqueClasses = new HashSet<>();
        Set<OWLClass>  classesToRemove = new HashSet<>();
        for(OWLClass clazz: classes){
            String nameClass = clazz.getIRI().getShortForm();
            String array[] = nameClass.split("_");
            if(array.length <= 1){
                if(babelnetService.existWord(nameClass)){
                    uniqueClasses.add(clazz);
                } else {
                    classesToRemove.add(clazz);
                }
            }
        }
        removeClasses(classesToRemove);
        System.out.println("cont:" + uniqueClasses.size());

        return uniqueClasses;
    }

    public  Set<OWLClass> getNoUniqueClasses() {
        Set<OWLClass> classes = ontology.getClassesInSignature();
        Set<OWLClass>  noUniqueClasses = new HashSet<>();
        for(OWLClass clazz: classes){
            String nameClass = clazz.getIRI().getShortForm();
            String array[] = nameClass.split("_");
            if(array.length > 1) {
                noUniqueClasses.add(clazz);
            }
        }
        System.out.println("cont no unique:" + noUniqueClasses.size());
        return noUniqueClasses;
    }

    public void getSimilarClasses(String path){
        addUniqueClass(path);
        Set<OWLClass> uniqueClasses = getUniqueClasses();
        Set<OWLClass> noUniqueClasses = getNoUniqueClasses();
        Set<OWLClass> classToRemove = new HashSet<>();
        try {
            for (OWLClass clazz : uniqueClasses) {
                Set<OWLClass> collect = noUniqueClasses.stream()
                    .filter(s -> s.getIRI().getShortForm().startsWith(clazz.getIRI().getShortForm()))
                    .collect(Collectors.toSet());
                if (collect.size() > 0) {
                    for(OWLClass subclass: noUniqueClasses){
                        OWLDataFactory ft = manager.getOWLDataFactory();
                        String split = clazz.getIRI().getShortForm();
                        String arraySubClass[] = subclass.getIRI().getShortForm().split(split);
                        if(arraySubClass.length > 1){
                            String nameIndividual = arraySubClass[1];
                            nameIndividual = nameIndividual.replaceFirst("_", "");
                            if(!createIndividual(nameIndividual)){
                                classToRemove.add(subclass);
                                OWLIndividual individual = ft.getOWLNamedIndividual(IRI.create(clazz.getIRI().getNamespace() + nameIndividual));
                                OWLClassAssertionAxiom ax = ft.getOWLClassAssertionAxiom(clazz, individual);
                                AddAxiom addAxiom = new AddAxiom(ontology, ax);
                                manager.applyChange(addAxiom);
                            }

                        }
                    }
                    removeClasses(classToRemove);
                }
            }
            removeEqualsClassAndIndividuals();
            removeClassesWithoutIndividuals();
            removeProperties();
            removePropertiesWithoutRange();
            saveOntology(path);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }

    public boolean createIndividual(String individual){
        Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();
        String words[] = individual.split("_");
        if (words.length <= 1) {
            for (OWLNamedIndividual namedIndividual : individuals) {
                if (namedIndividual.getIRI().getShortForm().equals(individual))
                    return true;
            }
        }
        return false;
    }

    public void createClass(String clazz){
        OWLDataFactory ft = manager.getOWLDataFactory();
        OWLClassAssertionAxiom ax = ft.getOWLClassAssertionAxiom(clazz);
        AddAxiom addAxiom = new AddAxiom(ontology, ax);
        manager.applyChange(addAxiom);
    }

    public void removeEqualsClassAndIndividuals(){
        Set<OWLClass> classes = ontology.getClassesInSignature();
        Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();
        Set<OWLClass> collect = new HashSet<>();
        for(OWLNamedIndividual individual: individuals){
          collect = classes.stream()
                    .filter(s -> s.getIRI().getShortForm().startsWith(individual.getIRI().getShortForm()))
                    .collect(Collectors.toSet());
        }
        removeClasses(collect);
    }

    public void removeClassesWithoutIndividuals(){
        Set<OWLClass> classesToRemove = new HashSet<>();
        for (OWLClass c : ontology.getClassesInSignature()) {
            Set<OWLIndividual> individuals = c.getIndividuals(ontology);
            System.out.println("classs:" + c.getIRI().getShortForm());
            if(individuals.isEmpty()){
                System.out.println("classs sin ind:" + c.getIRI().getShortForm());
               classesToRemove.add(c);
            }
        }
        removeClasses(classesToRemove);
        System.out.println("Clases removidas...");
    }

    public void  addUniqueClass(String path){
        Set<OWLClass> classes = ontology.getClassesInSignature();
        OWLDataFactory ft = manager.getOWLDataFactory();
        for(OWLClass clazz: classes){
           String[] words = clazz.getIRI().getShortForm().split("_");
           if(words.length > 0){
               String nameClass = words[0];
               OWLEntity entity = ft.getOWLEntity(EntityType.CLASS, IRI.create("#"+nameClass));
               OWLAxiom declare = ft.getOWLDeclarationAxiom(entity);
               manager.addAxiom(ontology,declare);
               try {
                   saveOntology(path);
               } catch (OWLOntologyStorageException e) {
                   e.printStackTrace();
               }
           }
        }
    }

    public  Set<OWLClass> getClassesWithMaxLength() {
        Set<OWLClass> classes = ontology.getClassesInSignature();
        Set<OWLClass> classesToRemove = new HashSet<>();
        for(OWLClass clazz : classes){
            int contSuperclasses = 0;
            if(!clazz.isOWLThing()){
                NodeSet<OWLClass> superclasses = reasoner.getSuperClasses(clazz, true);
                contSuperclasses = !superclasses.isEmpty() ? superclasses.getNodes().size() : 0;
                if(contSuperclasses > 5) {
                    classesToRemove.add(clazz);
                }
            }
        }
        return classesToRemove;
    }

    public int getMaxLength(){
        List<Integer> arrayLengths = new ArrayList<>();
        Set<OWLClass> classes = ontology.getClassesInSignature();
        for(OWLClass clazz : classes){
            if(!clazz.isOWLThing()){
                NodeSet<OWLClass> superclasses = reasoner.getSuperClasses(clazz, true);
                arrayLengths.add(superclasses.getNodes().size());
            }
        }
        return arrayLengths.stream()
                .mapToInt(v -> v)
                .max().orElseThrow(NoSuchElementException::new);
    }

    public void removeClasses(Set<OWLClass> classes){
        System.out.println("Number of classes before: " + ontology.getClassesInSignature().size());
        OWLEntityRemover remover = new OWLEntityRemover(manager, Collections.singleton(ontology));
        for (OWLClass ind : classes) {
            ind.accept(remover);
        }
        manager.applyChanges(remover.getChanges());
        System.out.println("Number of classes: " + ontology.getClassesInSignature().size());
       remover.reset();
    }

    public void removeProperties(){
        Set<OWLObjectProperty> properties = ontology.getObjectPropertiesInSignature();
        OWLEntityRemover remover = new OWLEntityRemover(manager, Collections.singleton(ontology));
        for(OWLClass clazz: ontology.getClassesInSignature()){
            for (OWLObjectProperty property : properties) {
                String nameClass = clazz.getIRI().getShortForm().replace("_c", "");
                String nameProperty = property.getIRI().getShortForm().replace("_r", "");
                String words[] = nameProperty.split("_");
                if(words.length <=1 || nameClass.equals(nameProperty)){
                    System.out.println("propiedad:" + property.getIRI().getShortForm());
                    property.accept(remover);
                }

            }
        }

        manager.applyChanges(remover.getChanges());
        System.out.println("Number of properties: " + ontology.getObjectPropertiesInSignature().size());
        remover.reset();
    }


    public void removePropertiesWithoutRange(){
        Set<OWLObjectProperty> properties = ontology.getObjectPropertiesInSignature();
        OWLEntityRemover remover = new OWLEntityRemover(manager, Collections.singleton(ontology));
        List<String> arrayPrefixProperties = new ArrayList<>();
        for (OWLObjectProperty property : properties) {
           String nameProperty = property.getIRI().getShortForm().replace("_r", "");
           String prefix = nameProperty.split("_")[0];
           if(prefix.length() > 1){
              arrayPrefixProperties.add(prefix);
           }
        }

        for(String prefix: arrayPrefixProperties){
            for (OWLObjectProperty property : properties){
                String nameProperty = property.getIRI().getShortForm().replace("_r", "");
                Set<OWLClassExpression> domains = property.getDomains(ontology);
                Set<OWLClassExpression> ranges = property.getRanges(ontology);
                boolean withoutRanges = domains.isEmpty() && ranges.isEmpty() ? true : false;
                if(nameProperty.startsWith(prefix) && withoutRanges){
                   property.accept(remover);
                }
            }
        }

        manager.applyChanges(remover.getChanges());
        System.out.println("Number of properties: " + ontology.getObjectPropertiesInSignature().size());
        remover.reset();
    }

    public List<String> getNodesConnectedFromBabelnet(){
        Set<OWLClass> classesOntology = getClasses();
        List<String> classesList = getListNameClasses(classesOntology);
        return classesList.stream()
                .filter(clazz -> babelnetService.existWord(clazz))
                .collect(Collectors.toList());
    }

    public void transferKnowledge(String pathOntology){
        List<String> nodesConnected = getNodesConnectedFromBabelnet();
        if(nodesConnected != null) {
            nodesConnected.forEach(node -> {
                List<String> listIdsNodes = babelnetService.getIds(node);
                if(!listIdsNodes.isEmpty()){
                    for(String id: listIdsNodes){
                        JsonNode relationsNode = babelnetService.getRelations(id);
                        String relation = relationsNode.get("shortName").asText();
                        if(relation.equals("is-a")){
                            createIndividual(relationsNode.get("target").asText());
                        }
                        if(relation.equals("subclass-of")){
                            createClass(relationsNode.get("target").asText());
                        }
                    }
                }
            });

        }
    }

    public int getCountAxioms(){
       return ontology.getAxiomCount();
    }

    public OWLOntology getOntology() {
        return ontology;
    }

    public void setOntology(OWLOntology ontology) {
        this.ontology = ontology;
    }

    public OWLReasonerFactory getFactory() {
        return factory;
    }

    public void setFactory(OWLReasonerFactory factory) {
        this.factory = factory;
    }


    public OWLOntologyManager getManager() {
        return manager;
    }
}
