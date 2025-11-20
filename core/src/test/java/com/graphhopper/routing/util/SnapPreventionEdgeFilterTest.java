package com.graphhopper.routing.util;

import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.RoadClass;
import com.graphhopper.routing.ev.RoadEnvironment;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeIteratorState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static com.graphhopper.routing.ev.RoadEnvironment.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SnapPreventionEdgeFilterTest {

    @Test
    public void accept() {
        EdgeFilter trueFilter = edgeState -> true;
        EncodingManager em = new EncodingManager.Builder().add(RoadClass.create()).add(RoadEnvironment.create()).build();
        EnumEncodedValue<RoadClass> rcEnc = em.getEnumEncodedValue(RoadClass.KEY, RoadClass.class);
        EnumEncodedValue<RoadEnvironment> reEnc = em.getEnumEncodedValue(RoadEnvironment.KEY, RoadEnvironment.class);
        SnapPreventionEdgeFilter filter = new SnapPreventionEdgeFilter(trueFilter, rcEnc, reEnc, Arrays.asList("motorway", "ferry"));
        BaseGraph graph = new BaseGraph.Builder(em).create();
        EdgeIteratorState edge = graph.edge(0, 1).setDistance(1);

        assertTrue(filter.accept(edge));
        edge.set(reEnc, RoadEnvironment.FERRY);
        assertFalse(filter.accept(edge));
        edge.set(reEnc, RoadEnvironment.FORD);
        assertTrue(filter.accept(edge));

        edge.set(rcEnc, RoadClass.RESIDENTIAL);
        assertTrue(filter.accept(edge));
        edge.set(rcEnc, RoadClass.MOTORWAY);
        assertFalse(filter.accept(edge));
    }

    //  Tests TÂCHE 3
    //  Mockito : simuler 2 classes

    @Mock
    private EdgeFilter baseFilter;

    @Mock
    private EnumEncodedValue<RoadClass> rcEncMock;

    @Mock
    private EnumEncodedValue<RoadEnvironment> reEncMock;

    @Mock
    private EdgeIteratorState edgeMock;

    /**
     * Test #1
     * Intention :
     *  S’assurer que le filtre respecte le court-circuit :
     *  si le filtre de base refuse, on refuse sans regarder les encodages.
     *
     * Données de test :
     *  - Filtre interne toujours false.
     *  - Arête quelconque (rcEncMock=MOTORWAY, reEncMock=TUNNEL pour montrer que ça n’a pas d’effet).
     *
     * Oracle :
     *  - accept(e) == false dans tous les cas (court-circuit).
     */
    @Test
    public void baseFilter_shortCircuits() {

        // Arrange: le filtre interne refuse toujours
        when(baseFilter.accept(edgeMock)).thenReturn(false);

        var filter = new SnapPreventionEdgeFilter(
                baseFilter,
                rcEncMock,
                reEncMock,
                List.of()
        );

        // Act
        boolean result = filter.accept(edgeMock);

        // Assert
        assertFalse(result, "Le filtre doit refuser dès que le filtre interne refuse.");

        // Vérifier qu’on ne lit jamais les encodages (court-circuit)
        verify(edgeMock, never()).get(rcEncMock);
        verify(edgeMock, never()).get(reEncMock);
    }

    /**
     * Test #2
     * Intention :
     *  Valider que chaque environnement est listé dans snapPreventions
     *  entraîne bien un refus.
     *
     * Données de test :
     *  - Test sur env = TUNNEL.
     *  - Filtre interne true, arête simulée avec reEncMock=TUNNEL.
     *  - snapPreventions = "tunnel" (minuscule).
     *
     * Oracle :
     *  - accept(e) == false car l'environnement est listé.
     */
    @Test
    public void refuse_eachRequestedEnvironment() {

        // Arrange: le filtre interne accepte
        when(baseFilter.accept(edgeMock)).thenReturn(true);

        // L'arête a un environnement TUNNEL
        when(edgeMock.get(reEncMock)).thenReturn(TUNNEL);

        // le constructeur attend des strings en minuscules : "tunnel"
        var filter = new SnapPreventionEdgeFilter(
                baseFilter,
                rcEncMock,
                reEncMock,
                List.of("tunnel")  // correspond à reEncMock=TUNNEL
        );

        // Act
        boolean result = filter.accept(edgeMock);

        // Assert
        assertFalse(result, "L'arête doit être refusée quand l'environnement simulé est TUNNEL.");

        verify(baseFilter, times(1)).accept(edgeMock);
        verify(edgeMock, times(1)).get(reEncMock);
        verify(edgeMock, never()).get(rcEncMock); // la classe n’est pas testée ici
    }
}

