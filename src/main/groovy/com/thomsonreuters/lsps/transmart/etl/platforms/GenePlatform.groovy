package com.thomsonreuters.lsps.transmart.etl.platforms
import com.thomsonreuters.lsps.transmart.etl.LogType
import com.thomsonreuters.lsps.transmart.etl.PlatformLoader
import com.thomsonreuters.lsps.transmart.etl.TransmartDatabaseFactory
import com.thomsonreuters.lsps.transmart.files.CsvLikeFile
import com.thomsonreuters.lsps.transmart.files.MetaInfoHeader
import com.thomsonreuters.lsps.db.core.Database
import com.thomsonreuters.lsps.transmart.util.PrepareIfRequired
import groovy.sql.BatchingPreparedStatementWrapper
import groovy.sql.Sql

import java.nio.file.Path

/**
 * Date: 19.09.2014
 * Time: 17:18
 */
abstract class GenePlatform implements PrepareIfRequired {
    String id
    String platformType
    String title
    String organism
    protected def config
    protected CsvLikeFile platformFile
    protected Database database

    GenePlatform(CsvLikeFile platformFile, String platformType, String id, config) {
        this.platformFile = platformFile
        this.platformType = platformType
        this.id = id
        this.config = config
        this.database = TransmartDatabaseFactory.newDatabase(config)
    }

    String getTitle() {
        prepareIfRequired()
        return title
    }

    String getOrganism() {
        prepareIfRequired()
        return organism
    }

    Path getFile() {
        return platformFile.file
    }

    protected Map fetchPlatformInfo() {
        config.logger.log("Fetching platform description from GEO")
        def txt = "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=${id}".toURL().getText()

        String title, organism
        def m = txt =~ /Title\<\/td\>\s*?\<td.*?\>(?:\[.+?\]\s*)*(.+?)\<\/td\>/
        if (m.size() > 0) {
            title = m[0][1]
        }

        m = txt =~ /Organism\<\/td\>\s*?\<td.*?\>\<a.+?\>(.+?)\<\/a\>/
        if (m.size() > 0) {
            organism = m[0][1]
        }
        return [title: title, organism: organism]
    }

    protected void readPlatformInfo() {
        def metaInfo = MetaInfoHeader.getMetaInfo(platformFile)
        title = title ?: metaInfo.PLATFORM_TITLE
        organism = organism ?: metaInfo.PLATFORM_SPECIES
        if (!title) {
            def info = fetchPlatformInfo()
            title = info.title
            if (info.organism) {
                organism = info.organism
            }
        }
        organism = organism ?: 'Homo Sapiens'
    }

    void prepare() {
        readPlatformInfo()
    }

    public abstract void cleanupTempTables(Sql sql)

    public abstract boolean isLoaded(Sql sql)

    protected int loadEachEntry(Sql sql, String insertStatement, Closure<List> toList) {
        int lineNum = 0
        sql.withTransaction {
            sql.withBatch(500, insertStatement) { BatchingPreparedStatementWrapper stmt ->
                eachEntry {
                    lineNum++
                    stmt.addBatch(toList(it))
                    config.logger.log(LogType.PROGRESS, "[${lineNum}]")
                }
            }
        }
        config.logger.log(LogType.PROGRESS, "")
        return lineNum
    }

    public void load(Sql sql, studyInfo) {
        new PlatformLoader(sql, config).doLoad(this, studyInfo)
    }

    public abstract int loadEntries(Sql sql)

    public abstract void eachEntry(Closure processEntry)
}
