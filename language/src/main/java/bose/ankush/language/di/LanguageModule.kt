package bose.ankush.language.di

import com.yariksoffice.lingver.Lingver
import dagger.Module
import dagger.Provides

@Module
class LanguageModule {

    @Provides
    fun provideLingverInstance(): Lingver = Lingver.getInstance()
}