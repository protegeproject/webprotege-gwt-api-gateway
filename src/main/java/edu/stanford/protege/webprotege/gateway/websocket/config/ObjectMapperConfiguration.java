package edu.stanford.protege.webprotege.gateway.websocket.config;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.stanford.protege.webprotege.change.OntologyChangedEvent;
import edu.stanford.protege.webprotege.event.*;
import edu.stanford.protege.webprotege.frame.*;
import edu.stanford.protege.webprotege.gateway.websocket.config.events.ParentsChangedEvent;
import edu.stanford.protege.webprotege.gateway.websocket.config.events.UpdateUiHistoryEvent;
import edu.stanford.protege.webprotege.hierarchy.EntityHierarchyChangedEvent;
import edu.stanford.protege.webprotege.issues.CommentPostedEvent;
import edu.stanford.protege.webprotege.issues.CommentUpdatedEvent;
import edu.stanford.protege.webprotege.issues.DiscussionThreadCreatedEvent;
import edu.stanford.protege.webprotege.issues.DiscussionThreadStatusChangedEvent;
import edu.stanford.protege.webprotege.jackson.*;
import edu.stanford.protege.webprotege.lang.DisplayNameSettingsChangedEvent;
import edu.stanford.protege.webprotege.permissions.PermissionsChangedEvent;
import edu.stanford.protege.webprotege.projectsettings.ProjectSettingsChangedEvent;
import edu.stanford.protege.webprotege.tag.EntityTagsChangedEvent;
import edu.stanford.protege.webprotege.tag.ProjectTagsChangedEvent;
import edu.stanford.protege.webprotege.watches.WatchAddedEvent;
import edu.stanford.protege.webprotege.watches.WatchRemovedEvent;
import org.semanticweb.owlapi.model.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.manchester.cs.owl.owlapi.*;

@Configuration
public class ObjectMapperConfiguration {


    @Bean
    public ObjectMapper objectMapper() {

            OWLDataFactory dataFactory = new OWLDataFactoryImpl();

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
            mapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            mapper.registerModule(new JavaTimeModule());
            mapper.registerModule(new Jdk8Module());
            mapper.registerModule(new GuavaModule());
            SimpleModule module = new SimpleModule();


            module.addSerializer(new EntityTypeSerializer());
            module.addDeserializer(EntityType.class, new EntityTypeDeserializer());

            module.setMixInAnnotation(OWLAnnotation.class, OWLAnnotationMixin.class);
            module.setMixInAnnotation(OWLAnnotationImpl.class, OWLAnnotationImplMixin.class);

            module.addSerializer(OWLLiteral.class, new OWLLiteralSerializer());
            module.addDeserializer(OWLLiteral.class, new OWLLiteralDeserializer(dataFactory));

            module.setMixInAnnotation(OWLOntologyID.class, OWLOntologyIDMixin.class);

            module.setMixInAnnotation(NodeID.class, NodeIDMixin.class);


            module.addSerializer(AxiomType.class, new AxiomTypeSerializer());
            module.addDeserializer(AxiomType.class, new AxiomTypeDeserializer());

            module.setMixInAnnotation(OWLAxiom.class, OWLAxiomMixin.class);

            module.setMixInAnnotation(OWLSubClassOfAxiom.class, OWLSubClassOfAxiomMixin.class);
            module.setMixInAnnotation(OWLSubClassOfAxiomImpl.class, OWLSubClassOfAxiomImplMixin.class);

            module.setMixInAnnotation(OWLEquivalentClassesAxiom.class, OWLEquivalentClassesAxiomMixin.class);
            module.setMixInAnnotation(OWLEquivalentClassesAxiomImpl.class, OWLEquivalentClassesAxiomImplMixin.class);

            module.setMixInAnnotation(OWLDisjointClassesAxiom.class, OWLDisjointClassesAxiomMixin.class);
            module.setMixInAnnotation(OWLDisjointClassesAxiomImpl.class, OWLDisjointClassesAxiomImplMixin.class);

            module.setMixInAnnotation(OWLDisjointUnionAxiom.class, OWLDisjointUnionAxiomMixin.class);
            module.setMixInAnnotation(OWLDisjointUnionAxiomImpl.class, OWLDisjointUnionAxiomImplMixin.class);

            module.setMixInAnnotation(OWLAnnotationAssertionAxiom.class, OWLAnnotationAssertionAxiomMixin.class);
            module.setMixInAnnotation(OWLAnnotationAssertionAxiomImpl.class, OWLAnnotationAssertionAxiomImplMixin.class);

            module.setMixInAnnotation(OWLClassAssertionAxiom.class, OWLClassAssertionAxiomMixin.class);
            module.setMixInAnnotation(OWLClassAssertionAxiomImpl.class, OWLClassAssertionAxiomImplMixin.class);

            module.setMixInAnnotation(OWLDeclarationAxiom.class, OWLDeclarationAxiomMixin.class);
            module.setMixInAnnotation(OWLDeclarationAxiomImpl.class, OWLDeclarationAxiomImplMixin.class);

            module.setMixInAnnotation(OWLObjectPropertyAssertionAxiom.class, OWLObjectPropertyAssertionAxiomMixin.class);
            module.setMixInAnnotation(OWLObjectPropertyAssertionAxiomImpl.class, OWLObjectPropertyAssertionAxiomImplMixin.class);

            module.setMixInAnnotation(OWLNegativeObjectPropertyAssertionAxiom.class, OWLNegativeObjectPropertyAssertionAxiomMixin.class);
            module.setMixInAnnotation(OWLNegativeObjectPropertyAssertionAxiomImpl.class, OWLNegativeObjectPropertyAssertionAxiomImplMixin.class);

            module.setMixInAnnotation(OWLDataPropertyAssertionAxiom.class, OWLDataPropertyAssertionAxiomMixin.class);
            module.setMixInAnnotation(OWLDataPropertyAssertionAxiomImpl.class, OWLDataPropertyAssertionAxiomImplMixin.class);

            module.setMixInAnnotation(OWLNegativeDataPropertyAssertionAxiom.class, OWLNegativeDataPropertyAssertionAxiomMixin.class);
            module.setMixInAnnotation(OWLNegativeDataPropertyAssertionAxiomImpl.class, OWLNegativeDataPropertyAssertionAxiomImplMixin.class);


            module.setMixInAnnotation(OWLSubObjectPropertyOfAxiom.class, OWLSubObjectPropertyOfAxiomMixin.class);
            module.setMixInAnnotation(OWLSubObjectPropertyOfAxiomImpl.class, OWLSubObjectPropertyOfAxiomImplMixin.class);

            module.setMixInAnnotation(OWLSubDataPropertyOfAxiom.class, OWLSubDataPropertyOfAxiomMixin.class);
            module.setMixInAnnotation(OWLSubDataPropertyOfAxiomImpl.class, OWLSubDataPropertyOfAxiomImplMixin.class);

            module.setMixInAnnotation(OWLSubAnnotationPropertyOfAxiom.class, OWLSubAnnotationPropertyOfAxiomMixin.class);
            module.setMixInAnnotation(OWLSubAnnotationPropertyOfAxiomImpl.class, OWLSubAnnotationPropertyOfAxiomImplMixin.class);

            module.setMixInAnnotation(OWLFunctionalObjectPropertyAxiom.class, OWLFunctionalObjectPropertyAxiomMixin.class);
            module.setMixInAnnotation(OWLFunctionalObjectPropertyAxiomImpl.class, OWLFunctionalObjectPropertyAxiomImplMixin.class);

            module.setMixInAnnotation(OWLFunctionalDataPropertyAxiom.class, OWLFunctionalDataPropertyAxiomMixin.class);
            module.setMixInAnnotation(OWLFunctionalDataPropertyAxiomImpl.class, OWLFunctionalDataPropertyAxiomImplMixin.class);

            module.setMixInAnnotation(OWLInverseFunctionalObjectPropertyAxiom.class, OWLInverseFunctionalObjectPropertyAxiomMixin.class);
            module.setMixInAnnotation(OWLInverseFunctionalObjectPropertyAxiomImpl.class, OWLInverseFunctionalObjectPropertyAxiomImplMixin.class);

            module.setMixInAnnotation(OWLAsymmetricObjectPropertyAxiom.class, OWLAsymmetricObjectPropertyAxiomMixin.class);
            module.setMixInAnnotation(OWLAsymmetricObjectPropertyAxiomImpl.class, OWLAsymmetricObjectPropertyAxiomImplMixin.class);

            module.setMixInAnnotation(OWLSymmetricObjectPropertyAxiom.class, OWLSymmetricObjectPropertyAxiomMixin.class);
            module.setMixInAnnotation(OWLSymmetricObjectPropertyAxiomImpl.class, OWLSymmetricObjectPropertyAxiomImplMixin.class);

            module.setMixInAnnotation(OWLTransitiveObjectPropertyAxiom.class, OWLTransitiveObjectPropertyAxiomMixin.class);
            module.setMixInAnnotation(OWLTransitiveObjectPropertyAxiomImpl.class, OWLTransitiveObjectPropertyAxiomImplMixin.class);

            module.setMixInAnnotation(OWLIrreflexiveObjectPropertyAxiom.class, OWLIrreflexiveObjectPropertyAxiomMixin.class);
            module.setMixInAnnotation(OWLIrreflexiveObjectPropertyAxiomImpl.class, OWLIrreflexiveObjectPropertyAxiomImplMixin.class);

            module.setMixInAnnotation(OWLSameIndividualAxiom.class, OWLSameIndividualAxiomMixin.class);
            module.setMixInAnnotation(OWLSameIndividualAxiomImpl.class, OWLSameIndividualAxiomImplMixin.class);

            module.setMixInAnnotation(OWLDifferentIndividualsAxiom.class, OWLDifferentIndividualsAxiomMixin.class);
            module.setMixInAnnotation(OWLDifferentIndividualsAxiomImpl.class, OWLDifferentIndividualsAxiomImplMixin.class);

            module.setMixInAnnotation(OWLDisjointObjectPropertiesAxiom.class, OWLDisjointObjectPropertiesAxiomMixin.class);
            module.setMixInAnnotation(OWLDisjointObjectPropertiesAxiomImpl.class, OWLDisjointObjectPropertiesAxiomImplMixin.class);

            module.setMixInAnnotation(OWLEquivalentObjectPropertiesAxiom.class, OWLEquivalentObjectPropertiesAxiomMixin.class);
            module.setMixInAnnotation(OWLEquivalentObjectPropertiesAxiomImpl.class, OWLEquivalentObjectPropertiesAxiomImplMixin.class);

            module.setMixInAnnotation(OWLInverseObjectPropertiesAxiom.class, OWLInverseObjectPropertiesAxiomMixin.class);
            module.setMixInAnnotation(OWLInverseObjectPropertiesAxiomImpl.class, OWLInverseObjectPropertiesAxiomImplMixin.class);

            module.setMixInAnnotation(OWLDisjointDataPropertiesAxiom.class, OWLDisjointDataPropertiesAxiomMixin.class);
            module.setMixInAnnotation(OWLDisjointDataPropertiesAxiomImpl.class, OWLDisjointDataPropertiesAxiomImplMixin.class);

            module.setMixInAnnotation(OWLEquivalentDataPropertiesAxiom.class, OWLEquivalentDataPropertiesAxiomMixin.class);
            module.setMixInAnnotation(OWLEquivalentDataPropertiesAxiomImpl.class, OWLEquivalentDataPropertiesAxiomImplMixin.class);

            module.setMixInAnnotation(OWLObjectPropertyRangeAxiom.class, OWLObjectPropertyRangeAxiomMixin.class);
            module.setMixInAnnotation(OWLObjectPropertyRangeAxiomImpl.class, OWLObjectPropertyRangeAxiomImplMixin.class);

            module.setMixInAnnotation(OWLObjectPropertyDomainAxiom.class, OWLObjectPropertyDomainAxiomMixin.class);
            module.setMixInAnnotation(OWLObjectPropertyDomainAxiomImpl.class, OWLObjectPropertyDomainAxiomImplMixin.class);

            module.setMixInAnnotation(OWLDataPropertyRangeAxiom.class, OWLDataPropertyRangeAxiomMixin.class);
            module.setMixInAnnotation(OWLDataPropertyRangeAxiomImpl.class, OWLDataPropertyRangeAxiomImplMixin.class);

            module.setMixInAnnotation(OWLDataPropertyDomainAxiom.class, OWLDataPropertyDomainAxiomMixin.class);
            module.setMixInAnnotation(OWLDataPropertyDomainAxiomImpl.class, OWLDataPropertyDomainAxiomImplMixin.class);

            module.setMixInAnnotation(OWLAnnotationPropertyDomainAxiom.class, OWLAnnotationPropertyDomainAxiomMixin.class);
            module.setMixInAnnotation(OWLAnnotationPropertyDomainAxiomImpl.class, OWLAnnotationPropertyDomainAxiomImplMixin.class);

            module.setMixInAnnotation(OWLAnnotationPropertyRangeAxiom.class, OWLAnnotationPropertyRangeAxiomMixin.class);
            module.setMixInAnnotation(OWLAnnotationPropertyRangeAxiomImpl.class, OWLAnnotationPropertyRangeAxiomImplMixin.class);

            module.setMixInAnnotation(OWLDatatypeDefinitionAxiom.class, OWLDatatypeDefinitionAxiomMixin.class);
            module.setMixInAnnotation(OWLDatatypeDefinitionAxiomImpl.class, OWLDatatypeDefinitionAxiomImplMixin.class);

            module.setMixInAnnotation(OWLHasKeyAxiom.class, OWLHasKeyAxiomMixin.class);
            module.setMixInAnnotation(OWLHasKeyAxiomImpl.class, OWLHasKeyAxiomImplMixin.class);

            module.setMixInAnnotation(OWLDataPropertyExpression.class, OWLDataPropertyExpressionMixin.class);

            module.setMixInAnnotation(OWLPropertyExpression.class, OWLPropertyExpressionMixin.class);
            module.setMixInAnnotation(OWLObjectPropertyExpression.class, OWLObjectPropertyExpressionMixin.class);
            module.setMixInAnnotation(OWLObjectInverseOf.class, OWLObjectInverseOfMixin.class);
            module.setMixInAnnotation(OWLObjectInverseOfImpl.class, OWLObjectInverseOfMixinImpl.class);

            module.setMixInAnnotation(OWLDataRange.class, OWLDataRangeMixin.class);

            module.setMixInAnnotation(OWLEntity.class, OWLEntityMixin.class);
            // We need this to support legacy representation of entities that use "type" instead of "@type"
            // for the field name that identifies the type id
            module.addDeserializer(OWLEntity.class, new OWLEntityDeserializer<>(dataFactory));

            module.setMixInAnnotation(OWLClass.class, OWLClassMixin.class);
            module.addDeserializer(OWLClassImpl.class, new OWLEntityDeserializer<>(dataFactory, EntityType.CLASS));

            module.setMixInAnnotation(OWLDatatype.class, OWLDatatypeMixin.class);
            module.addDeserializer(OWLDatatypeImpl.class, new OWLEntityDeserializer<>(dataFactory, EntityType.DATATYPE));

            module.setMixInAnnotation(OWLProperty.class, OWLPropertyMixIn.class);
            module.addDeserializer(OWLProperty.class, new OWLEntityDeserializer<>(dataFactory));

            module.setMixInAnnotation(OWLObjectProperty.class, OWLObjectPropertyMixin.class);
            module.addDeserializer(OWLObjectPropertyImpl.class, new OWLEntityDeserializer<>(dataFactory, EntityType.OBJECT_PROPERTY));

            module.setMixInAnnotation(OWLDataProperty.class, OWLDataPropertyMixin.class);
            module.addDeserializer(OWLDataPropertyImpl.class, new OWLEntityDeserializer<>(dataFactory, EntityType.DATA_PROPERTY));
//
            module.setMixInAnnotation(OWLAnnotationProperty.class, OWLAnnotationPropertyMixin.class);
            module.addDeserializer(OWLAnnotationPropertyImpl.class, new OWLEntityDeserializer<>(dataFactory, EntityType.ANNOTATION_PROPERTY));

            module.setMixInAnnotation(OWLIndividual.class, OWLIndividualMixin.class);

            module.setMixInAnnotation(OWLNamedIndividual.class, OWLNamedIndividualMixin.class);
            module.addDeserializer(OWLNamedIndividualImpl.class, new OWLEntityDeserializer<>(dataFactory, EntityType.NAMED_INDIVIDUAL));

            module.setMixInAnnotation(OWLAnonymousIndividual.class, OWLAnonymousIndividualMixin.class);
            module.setMixInAnnotation(OWLAnonymousIndividualImpl.class, OWLAnonymousIndividualImplMixin.class);

            module.setMixInAnnotation(OWLObjectInverseOf.class, OWLObjectInverseOfMixin.class);
            module.setMixInAnnotation(OWLObjectInverseOfImpl.class, OWLObjectInverseOfMixin.class);

            module.setMixInAnnotation(OWLClassExpression.class, OWLClassExpressionMixin.class);

            module.setMixInAnnotation(OWLObjectSomeValuesFrom.class, OWLObjectSomeValuesFromMixin.class);
            module.setMixInAnnotation(OWLObjectSomeValuesFromImpl.class, OWLObjectSomeValuesFromImplMixin.class);

            module.setMixInAnnotation(OWLObjectAllValuesFrom.class, OWLObjectAllValuesFromMixin.class);
            module.setMixInAnnotation(OWLObjectAllValuesFromImpl.class, OWLObjectAllValuesFromImplMixin.class);

            module.setMixInAnnotation(OWLObjectMinCardinality.class, OWLObjectMinCardinalityMixin.class);
            module.setMixInAnnotation(OWLObjectMinCardinalityImpl.class, OWLObjectMinCardinalityImplMixin.class);

            module.setMixInAnnotation(OWLObjectHasValue.class, OWLObjectHasValueMixin.class);
            module.setMixInAnnotation(OWLObjectHasValueImpl.class, OWLObjectHasValueImplMixin.class);


            module.setMixInAnnotation(OWLDataSomeValuesFrom.class, OWLDataSomeValuesFromMixin.class);
            module.setMixInAnnotation(OWLDataSomeValuesFromImpl.class, OWLDataSomeValuesFromImplMixin.class);

            module.setMixInAnnotation(OWLDataAllValuesFrom.class, OWLDataAllValuesFromMixin.class);
            module.setMixInAnnotation(OWLDataAllValuesFromImpl.class, OWLDataAllValuesFromImplMixin.class);

            module.setMixInAnnotation(OWLDataHasValue.class, OWLDataHasValueMixin.class);
            module.setMixInAnnotation(OWLDataHasValueImpl.class, OWLDataHasValueImplMixin.class);

            module.setMixInAnnotation(OWLDataMinCardinality.class, OWLDataMinCardinalityMixin.class);
            module.setMixInAnnotation(OWLDataMinCardinalityImpl.class, OWLDataMinCardinalityImplMixin.class);

            module.setMixInAnnotation(OWLDataMaxCardinality.class, OWLDataMaxCardinalityMixin.class);
            module.setMixInAnnotation(OWLDataMaxCardinalityImpl.class, OWLDataMaxCardinalityImplMixin.class);

            module.setMixInAnnotation(OWLDataExactCardinality.class, OWLDataExactCardinalityMixin.class);
            module.setMixInAnnotation(OWLDataExactCardinalityImpl.class, OWLDataExactCardinalityImplMixin.class);

            module.setMixInAnnotation(OWLObjectMinCardinality.class, OWLObjectMinCardinalityMixin.class);
            module.setMixInAnnotation(OWLObjectMinCardinalityImpl.class, OWLObjectMinCardinalityImplMixin.class);

            module.setMixInAnnotation(OWLObjectMaxCardinality.class, OWLObjectMaxCardinalityMixin.class);
            module.setMixInAnnotation(OWLObjectMaxCardinalityImpl.class, OWLObjectMaxCardinalityImplMixin.class);

            module.setMixInAnnotation(OWLObjectExactCardinality.class, OWLObjectExactCardinalityMixin.class);
            module.setMixInAnnotation(OWLObjectExactCardinalityImpl.class, OWLObjectExactCardinalityImplMixin.class);

            module.setMixInAnnotation(OWLObjectHasSelf.class, OWLObjectHasSelfMixin.class);
            module.setMixInAnnotation(OWLObjectHasSelfImpl.class, OWLObjectHasSelfImplMixin.class);

            module.setMixInAnnotation(OWLObjectIntersectionOf.class, OWLObjectIntersectionOfMixin.class);
            module.setMixInAnnotation(OWLObjectIntersectionOfImpl.class, OWLObjectIntersectionOfImplMixin.class);

            module.setMixInAnnotation(OWLObjectUnionOf.class, OWLObjectUnionOfMixin.class);
            module.setMixInAnnotation(OWLObjectUnionOfImpl.class, OWLObjectUnionOfImplMixin.class);

            module.setMixInAnnotation(OWLObjectComplementOf.class, OWLObjectComplementOfMixin.class);
            module.setMixInAnnotation(OWLObjectComplementOfImpl.class, OWLObjectComplementOfImplMixin.class);

            module.setMixInAnnotation(OWLObjectOneOf.class, OWLObjectOneOfMixin.class);
            module.setMixInAnnotation(OWLObjectOneOfImpl.class, OWLObjectOneOfImplMixin.class);

            module.setMixInAnnotation(OWLDataIntersectionOf.class, OWLDataIntersectionOfMixin.class);
            module.setMixInAnnotation(OWLDataIntersectionOfImpl.class, OWLDataIntersectionOfImplMixin.class);

            module.setMixInAnnotation(OWLDataUnionOf.class, OWLDataUnionOfMixin.class);
            module.setMixInAnnotation(OWLDataUnionOfImpl.class, OWLDataUnionOfImplMixin.class);

            module.setMixInAnnotation(OWLDataComplementOf.class, OWLDataComplementOfMixin.class);
            module.setMixInAnnotation(OWLDataComplementOfImpl.class, OWLDataComplementOfImplMixin.class);

            module.setMixInAnnotation(OWLDataOneOf.class, OWLDataOneOfMixin.class);
            module.setMixInAnnotation(OWLDataOneOfImpl.class, OWLDataOneOfImplMixin.class);

            module.setMixInAnnotation(OWLDatatypeRestriction.class, OWLDatatypeRestrictionMixin.class);
            module.setMixInAnnotation(OWLDatatypeRestrictionImpl.class, OWLDatatypeRestrictionImplMixin.class);

            module.setMixInAnnotation(OWLFacetRestriction.class, OWLFacetRestrictionMixin.class);
            module.setMixInAnnotation(OWLFacetRestrictionImpl.class, OWLFacetRestrictionImplMixin.class);

            module.setMixInAnnotation(OWLLiteral.class, OWLLiteralMixin.class);

            module.setMixInAnnotation(IRI.class, IRIMixin.class);

            mapper.addHandler(new MissingTypeIdDeserializationProblemHandler());
            module.registerSubtypes(
                    BrowserTextChangedEvent.class,
                    ClassFrameChangedEvent.class,
                    DataPropertyFrameChangedEvent.class,
                    DatatypeFrameChangedEvent.class,
                    NamedIndividualFrameChangedEvent.class,
                    ObjectPropertyFrameChangedEvent.class,
                    EntityDeprecationStatusChangedEvent.class,
                    AnnotationPropertyFrameChangedEvent.class,
                    LargeNumberOfChangesEvent.class,
                    OntologyBrowserTextChangedEvent.class,
                    PermissionsChangedEvent.class,
                    ProjectChangedEvent.class,
                    ParentsChangedEvent.class,
                    UpdateUiHistoryEvent.class,
                    ProjectMovedToTrashEvent.class,
                    OntologyChangedEvent.class,
                    ProjectRemovedFromTrashEvent.class,
                    EntityHierarchyChangedEvent.class,
                    CommentPostedEvent.class,
                    CommentUpdatedEvent.class,
                    DiscussionThreadCreatedEvent.class,
                    DiscussionThreadStatusChangedEvent.class,
                    DisplayNameSettingsChangedEvent.class,
                    ProjectSettingsChangedEvent.class,
                    EntityTagsChangedEvent.class,
                    ProjectTagsChangedEvent.class,
                    WatchAddedEvent.class,
                    WatchRemovedEvent.class);

            mapper.registerModule(module);

            return mapper;

    }


}
