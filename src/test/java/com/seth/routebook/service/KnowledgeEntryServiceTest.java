package com.seth.routebook.service;

import com.seth.routebook.domain.KnowledgeEntry;
import com.seth.routebook.domain.Route;
import com.seth.routebook.domain.Stop;
import com.seth.routebook.domain.enums.KnowledgeCategory;
import com.seth.routebook.dto.KnowledgeEntryDto;
import com.seth.routebook.exception.InvalidKnowledgeEntryTargetException;
import com.seth.routebook.exception.ResourceNotFoundException;
import com.seth.routebook.repository.KnowledgeEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Route/Stop XOR rule - the single most important
 * business rule in RouteBook. A KnowledgeEntry must target exactly one
 * of Route or Stop; these tests lock that behavior in.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeEntryServiceTest {

    @Mock
    private KnowledgeEntryRepository knowledgeEntryRepository;

    @Mock
    private RouteService routeService;

    @Mock
    private StopService stopService;

    @InjectMocks
    private KnowledgeEntryService knowledgeEntryService;

    private Route route;
    private Stop stop;

    @BeforeEach
    void setUp() {
        route = new Route();
        route.setId(1L);
        route.setName("Route 14 - Frankfort");

        stop = new Stop();
        stop.setId(1L);
        stop.setCustomerName("Nucor Steel");
    }

    @Test
    void create_withOnlyRouteId_succeeds() {
        when(routeService.getEntityOrThrow(1L)).thenReturn(route);
        when(knowledgeEntryRepository.save(any(KnowledgeEntry.class)))
                .thenAnswer(invocation -> {
                    KnowledgeEntry entry = invocation.getArgument(0);
                    entry.setId(1L);
                    return entry;
                });

        KnowledgeEntryDto request = new KnowledgeEntryDto(
                null, "Hazard note", "Body text", KnowledgeCategory.HAZARD, 1L, null);

        KnowledgeEntryDto result = knowledgeEntryService.create(request);

        assertThat(result.routeId()).isEqualTo(1L);
        assertThat(result.stopId()).isNull();
        verify(stopService, never()).getEntityOrThrow(any());
    }

    @Test
    void create_withOnlyStopId_succeeds() {
        when(stopService.getEntityOrThrow(1L)).thenReturn(stop);
        when(knowledgeEntryRepository.save(any(KnowledgeEntry.class)))
                .thenAnswer(invocation -> {
                    KnowledgeEntry entry = invocation.getArgument(0);
                    entry.setId(2L);
                    return entry;
                });

        KnowledgeEntryDto request = new KnowledgeEntryDto(
                null, "Gate code", "4471#", KnowledgeCategory.GATE_CODE, null, 1L);

        KnowledgeEntryDto result = knowledgeEntryService.create(request);

        assertThat(result.stopId()).isEqualTo(1L);
        assertThat(result.routeId()).isNull();
        verify(routeService, never()).getEntityOrThrow(any());
    }

    @Test
    void create_withBothRouteAndStopId_throwsInvalidKnowledgeEntryTargetException() {
        KnowledgeEntryDto request = new KnowledgeEntryDto(
                null, "Bad entry", "Body", KnowledgeCategory.OTHER, 1L, 1L);

        assertThatThrownBy(() -> knowledgeEntryService.create(request))
                .isInstanceOf(InvalidKnowledgeEntryTargetException.class)
                .hasMessageContaining("exactly one");

        verifyNoInteractions(knowledgeEntryRepository);
    }

    @Test
    void create_withNeitherRouteNorStopId_throwsInvalidKnowledgeEntryTargetException() {
        KnowledgeEntryDto request = new KnowledgeEntryDto(
                null, "Bad entry", "Body", KnowledgeCategory.OTHER, null, null);

        assertThatThrownBy(() -> knowledgeEntryService.create(request))
                .isInstanceOf(InvalidKnowledgeEntryTargetException.class)
                .hasMessageContaining("exactly one");

        verifyNoInteractions(knowledgeEntryRepository);
    }

    @Test
    void create_withNonexistentRouteId_propagatesResourceNotFoundException() {
        when(routeService.getEntityOrThrow(999L))
                .thenThrow(new ResourceNotFoundException("No route found with id 999"));

        KnowledgeEntryDto request = new KnowledgeEntryDto(
                null, "Title", "Body", KnowledgeCategory.OTHER, 999L, null);

        assertThatThrownBy(() -> knowledgeEntryService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(knowledgeEntryRepository);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(knowledgeEntryRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> knowledgeEntryService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
