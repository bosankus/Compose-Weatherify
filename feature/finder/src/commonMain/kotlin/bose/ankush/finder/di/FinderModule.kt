package bose.ankush.finder.di

import bose.ankush.finder.data.repository.FinderRepositoryImpl
import bose.ankush.finder.data.usecase.DeleteLocationUseCaseImpl
import bose.ankush.finder.data.usecase.GetSavedLocationsUseCaseImpl
import bose.ankush.finder.data.usecase.SaveLocationUseCaseImpl
import bose.ankush.finder.data.usecase.SearchPlacesUseCaseImpl
import bose.ankush.finder.domain.repository.FinderRepository
import bose.ankush.finder.domain.usecase.*
import bose.ankush.finder.presentation.placesearch.PlaceSearchViewModel
import bose.ankush.finder.presentation.savedlocations.SavedLocationsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for the finder feature — domain layer bindings.
 */
val finderDomainModule: Module =
    module {
        single<FinderRepository> { FinderRepositoryImpl(get()) }
        factory<SearchPlacesUseCase> { SearchPlacesUseCaseImpl(get()) }
        factory<GetSavedLocationsUseCase> { GetSavedLocationsUseCaseImpl(get()) }
        factory<SaveLocationUseCase> { SaveLocationUseCaseImpl(get()) }
        factory<DeleteLocationUseCase> { DeleteLocationUseCaseImpl(get()) }
    }

/** Koin module for the finder feature — presentation layer bindings. */
val finderViewModelModule: Module =
    module {
        viewModelOf(::SavedLocationsViewModel)
        viewModelOf(::PlaceSearchViewModel)
    }
