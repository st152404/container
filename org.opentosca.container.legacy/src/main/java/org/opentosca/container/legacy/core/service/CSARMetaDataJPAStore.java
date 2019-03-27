package org.opentosca.container.legacy.core.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFile;
import org.opentosca.container.core.common.NotFoundException;
import org.opentosca.container.core.common.UserException;
import org.opentosca.container.core.model.csar.backwards.FileSystemDirectory;
import org.opentosca.container.legacy.core.model.CSARContent;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.next.jpa.EntityManagerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages CSAR meta data in the database by using Eclipse Link (JPA).
 */
@Deprecated
public class CSARMetaDataJPAStore {

  private final static Logger LOG = LoggerFactory.getLogger(CSARMetaDataJPAStore.class);

  private EntityManager em;

  /**
   * Initializes JPA.
   */
  private void initJPA() {
    if (this.em == null) {
      this.em = EntityManagerProvider.createEntityManager();
    }
  }

  /**
   * This method is called when the garbage collector destroys the class. We will then manually close
   * the EntityManager / Factory and pass control back.
   */
  @Override
  protected void finalize() throws Throwable {
    this.em.close();
    super.finalize();
  }

  /**
   * Persists the meta data of CSAR {@code csarID}.
   *
   * @param csarID        of the CSAR.
   * @param toscaMetaFile - represents the content of the TOSCA meta file of the CSAR.
   */
  public void storeCSARMetaData(final CSARID csarID, final TOSCAMetaFile toscaMetaFile) {
    initJPA();
    LOG.debug("Storing meta data of CSAR \"{}\"...", csarID);

    // FIXME pass the actual directory of the CSAR root
    final CSARContent csar = new CSARContent(csarID, new FileSystemDirectory(Paths.get("")), toscaMetaFile);

    this.em.getTransaction().begin();
    this.em.persist(csar);
    this.em.getTransaction().commit();

    // clear the JPA 1st level cache
    this.em.clear();

    LOG.debug("Storing meta data of CSAR \"{}\" completed.", csarID);
  }

  /**
   * @param csarID of CSAR
   * @return {@code true} if meta data of CSAR {@code csarID} were found, otherwise {@code false}.
   */
  public boolean isCSARMetaDataStored(final CSARID csarID) {
    initJPA();
    LOG.debug("Checking if meta data of CSAR \"{}\" are stored...", csarID);

    final TypedQuery<CSARContent> query = this.em.createNamedQuery(CSARContent.csarsByCSARID, CSARContent.class);
    query.setParameter("csarID", csarID);

    try {
      query.getSingleResult();
      LOG.debug("Meta data of CSAR \"{}\" were found.", csarID);
      return true;
    } catch (NoResultException e) {
      LOG.debug("Meta data of CSAR \"{}\" were not found.", csarID);
      return false;
    }
  }

  /**
   * Retrieves the meta data of CSAR {@code csarID}.
   *
   * @param csarID of CSAR.
   * @return {@link CSARContent} that gives access to all files and directories and the TOSCA meta
   * file of the CSAR.
   * @throws UserException if meta data of CSAR {@code csarID} were not found.
   */
  public CSARContent getCSARMetaData(final CSARID csarID) throws UserException {
    initJPA();
    LOG.debug("Retrieving meta data of CSAR \"{}\"...", csarID);

    final TypedQuery<CSARContent> query = this.em.createNamedQuery(CSARContent.csarsByCSARID, CSARContent.class);
    query.setParameter("csarID", csarID);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      String message = String.format("Meta data of CSAR \"%s\" were not found.", csarID);
      LOG.debug(message);
      throw new NotFoundException(message);
    }
  }

  /**
   * @return CSAR IDs of all stored CSAR files.
   */
  public Set<CSARID> getCSARIDsMetaData() {
    initJPA();
    LOG.trace("Retrieving CSAR IDs of all stored CSARs...");
    final Query getCSARIDsQuery = this.em.createNamedQuery(CSARContent.getCSARIDs);

    @SuppressWarnings("unchecked") final List<CSARID> csarIDs = getCSARIDsQuery.getResultList();
    LOG.trace("{} CSAR ID(s) was / were found.", csarIDs.size());
    return new HashSet<>(csarIDs);
  }

  /**
   * Deletes the meta data of CSAR {@code csarID}.
   *
   * @param csarID of CSAR.
   * @throws UserException if meta data of CSAR {@code csarID} were not found.
   */
  public void deleteCSARMetaData(final CSARID csarID) throws UserException {
    initJPA();
    LOG.debug("Deleting meta data of CSAR \"{}\"...", csarID);
    final CSARContent csarContent = getCSARMetaData(csarID);

    this.em.getTransaction().begin();
    this.em.remove(csarContent);
    this.em.getTransaction().commit();

    LOG.debug("Deleting meta data of CSAR \"{}\" completed.", csarID);
  }

  /**
   * Persists / updates the storage provider ID of file {@code fileRelToCSARRoot} in CSAR
   * {@code csarID} to {@code storageProviderID}.
   *
   * @param csarID            of CSAR.
   * @param fileRelToCSARRoot - file relative to CSAR root.
   * @param storageProviderID of storage provider to set for file {@code fileRelToCSARRoot}.
   * @throws UserException if meta data of file {@code fileRelToCSARRoot} in CSAR {@code CSARID} were
   *                       not found.
   */
  public void storeFileStorageProviderIDOfCSAR(final CSARID csarID, final Path fileRelToCSARRoot,
                                               final String storageProviderID) throws UserException {

    initJPA();
    LOG.debug("Setting storage provider \"{}\" in meta data of file \"{}\" in CSAR \"{}\"...", storageProviderID, fileRelToCSARRoot, csarID);

    final Query storeStorageProviderIDByFileAndCSARIDQuery = this.em.createNamedQuery(CSARContent.storeStorageProviderIDByFileAndCSARID);

    storeStorageProviderIDByFileAndCSARIDQuery.setParameter(1, storageProviderID);
    storeStorageProviderIDByFileAndCSARIDQuery.setParameter(2, fileRelToCSARRoot.toString());
    storeStorageProviderIDByFileAndCSARIDQuery.setParameter(3, csarID.toString());

    this.em.getTransaction().begin();
    final int updatedFiles = storeStorageProviderIDByFileAndCSARIDQuery.executeUpdate();
    this.em.getTransaction().commit();

    if (updatedFiles > 0) {
      // After the execution of the native query we must manually
      // synchronize the persistence context with the database context.
      // For this will clear the 1st level cache
      this.em.clear();
      LOG.debug("Setting storage provider \"{}\" in meta data of file \"{}\" in CSAR \"{}\" completed.", storageProviderID, fileRelToCSARRoot, csarID);
    } else {
      throw new UserException("Meta data of file \"" + fileRelToCSARRoot + "\" of CSAR \"" + csarID + "\" were not found.");
    }
  }

  /**
   * @param csarID of CSAR.
   * @return Each file of CSAR {@code csarID} relative to CSAR root mapped to the ID of the storage
   * provider the file is stored on.
   * @throws UserException if file to storage provider ID mapping meta data of CSAR {@code csarID}
   *                       were not found.
   */
  public Map<Path, String> getFileToStorageProviderIDMap(final CSARID csarID) throws UserException {
    LOG.debug("Retrieving file to storage provider mapping meta data of CSAR \"{}\"...", csarID);
    initJPA();
    final Query getFileToStorageProviderIDMapQuery = this.em.createNamedQuery(CSARContent.getFileToStorageProviderIDMapByCSARID);
    getFileToStorageProviderIDMapQuery.setParameter("csarID", csarID);

    @SuppressWarnings("unchecked") final List<Object[]> fileToStorageProviderIDEntries = getFileToStorageProviderIDMapQuery.getResultList();
    if (fileToStorageProviderIDEntries.isEmpty()) {
      throw new UserException("Meta data of CSAR \"" + csarID + "\" were not found.");
    }

    final Map<Path, String> fileToStorageProviderIDMap = new HashMap<>();
    for (final Object[] fileToStorageProviderIDEntry : fileToStorageProviderIDEntries) {
      final Path file = (Path) fileToStorageProviderIDEntry[0];
      final String storageProviderID = (String) fileToStorageProviderIDEntry[1];
      fileToStorageProviderIDMap.put(file, storageProviderID);
    }

    LOG.debug("Retrieving file to storage provider mapping meta data of CSAR \"{}\" completed.", csarID);
    return fileToStorageProviderIDMap;
  }

  /**
   * @param csarID of CSAR.
   * @return Directories meta data of CSAR {@code csarID}.
   * @throws UserException if directories meta data of CSAR {@code csarID} were not found.
   */
  public Set<Path> getDirectories(final CSARID csarID) throws UserException {
    initJPA();
    LOG.debug("Retrieving directories meta data of CSAR \"{}\"...", csarID);

    final TypedQuery<CSARContent> getDirectoriesQuery = this.em.createNamedQuery(CSARContent.getDirectoriesByCSARID, CSARContent.class);
    getDirectoriesQuery.setParameter("csarID", csarID);

    final CSARContent result = getDirectoriesQuery.getSingleResult();
    if (result == null) {
      throw new UserException("Meta data of CSAR \"" + csarID + "\" were not found.");
    }

    final Set<Path> directories = result.getDirectoriesJpa();
    LOG.debug("Directories: {}", directories.size());
    LOG.debug("Retrieving directories meta data of CSAR \"{}\" completed.", csarID);
    return directories;
  }
}
