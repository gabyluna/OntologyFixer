/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package decisionmakertool.owl;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.impl.blackbox.checker.InconsistentOntologyExplanationGeneratorFactory;
import uk.ac.manchester.cs.jfact.JFactFactory;
import javax.faces.context.FacesContext;

public class OntologyUtil {

    private OWLOntology ontology;
    private OWLOntologyManager manager;
    private OWLReasonerFactory factory = null;
    private OWLReasoner reasoner;
    private OWLDataFactory dataFactory = null;

    public OntologyUtil() {

    }

    public OntologyUtil(String path) {
        try {
            File file = new File(path);
            manager = OWLManager.createOWLOntologyManager();
            ontology = manager.loadOntologyFromOntologyDocument(file);
            factory = new JFactFactory();
            reasoner = this.factory.createReasoner(ontology);
            dataFactory = manager.getOWLDataFactory();
        } catch (OWLOntologyCreationException ex) {
            Logger.getLogger(OntologyUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String validationConsistency(String path) {
        String answer = "";
        File file = new File(path);

        try {
            manager = OWLManager.createOWLOntologyManager();
            ontology = manager.loadOntologyFromOntologyDocument(file);
            manager = ontology.getOWLOntologyManager();
            dataFactory = manager.getOWLDataFactory();
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

    public void removeAxioms(String valueURI) throws OWLOntologyStorageException {
        //saveOntology("ontoFinalCopy.owl");
        System.out.println("Element:" + valueURI);
        OWLClass owlClass = manager . getOWLDataFactory().getOWLClass(IRI.create(valueURI));
        Set < OWLAxiom > axiomsToRemove =  new  HashSet <> ();
        for ( OWLAxiom axiom : ontology.getAxioms ()) {
            if (axiom.getSignature().toString().contains(valueURI)) {
                axiomsToRemove.add(axiom);
                System.out.println( " para eliminar de "  + ontology.getOntologyID ().getOntologyIRI () +  " : "  + axiom);
            }
        }
         manager.removeAxioms (ontology, axiomsToRemove);
         saveOntology("ontoQuickFix1.owl");
    }

    public void saveOntology(String nameOwl) throws OWLOntologyStorageException {
        String realpath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("/resources");
        String pathAutomaticOntology = realpath + "/" + nameOwl;
        File fileformated = new File(pathAutomaticOntology);
        //Save the ontology in a different format
        OWLOntologyFormat format = manager.getOntologyFormat(ontology);
        RDFXMLOntologyFormat owlxmlFormat = new RDFXMLOntologyFormat();
        if (format.isPrefixOWLOntologyFormat()) {
            owlxmlFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
        }
        manager.saveOntology(ontology, owlxmlFormat, IRI.create(fileformated.toURI()));
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

}