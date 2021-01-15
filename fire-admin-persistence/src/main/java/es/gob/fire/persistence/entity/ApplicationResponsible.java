/*
 * Este fichero forma parte de la plataforma de @firma.
 * La plataforma de @firma es de libre distribución cuyo código fuente puede ser consultado
 * y descargado desde http://administracionelectronica.gob.es
 *
 * Copyright 2005-2019 Gobierno de España
 * Este fichero se distribuye bajo las licencias EUPL versión 1.1, según las
 * condiciones que figuran en el fichero 'LICENSE.txt' que se acompaña.  Si se   distribuyera este
 * fichero individualmente, deben incluirse aquí las condiciones expresadas allí.
 */

/**
 * <b>File:</b><p>es.gob.afirma.persistence.configuration.model.pojo.XApplicationResponsiblePOJO.java.</p>
 * <b>Description:</b><p>Class that represents the representation of the <i>X_APPLICATION_RESPONSIBLE</i> database table as a Plain Old Java Object.</p>
 * <b>Project:</b><p>Horizontal platform of validation services of multiPKI certificates and electronic signature.</p>
 * <b>Date:</b><p>16/02/2017.</p>
 * @author Gobierno de España.
 * @version 1.0, 16/02/2017.
 */
package es.gob.fire.persistence.entity;

import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

/**
 * <p>Class that maps the <i>TB_RESPONSABLE_DE_APLICACIONES</i> database table as a Plain Old Java Object.</p>
 * <b>Project:</b><p>Application for monitoring services of @firma suite systems.</p>
 * @version 1.0, 12/01/2021.
 */
@Cacheable
@Entity
@Table(name = "TB_RESPONSABLE_DE_APLICACIONES")
public class ApplicationResponsible implements Serializable {
	
	/**
	 * Constant attribute that represents the serial version UID. 
	 */
	private static final long serialVersionUID = -844662441561734046L;

	/**
	 * Attribute that represents the object ID.
	 */
	private ApplicationResponsiblePK idApplicationResponsible;

	/**
	 * Attribute that represents the associated application.
	 */
	private Application application;

	/**
	 * Attribute that represents the associated responsible.
	 */
	private User responsible;

	/**
	 * Gets the value of the attribute {@link #idApplicationResponsible}.
	 * @return the value of the attribute {@link #idApplicationResponsible}.
	 */
	// CHECKSTYLE:OFF -- Checkstyle rule "Design for Extension" is not applied
	// because Hibernate JPA needs not final access methods.
	@EmbeddedId
	public ApplicationResponsiblePK getIdApplicationResponsible() {
		// CHECKSTYLE:ON
		return idApplicationResponsible;
	}

	/**
	 * Sets the value of the attribute {@link #idApplicationResponsible}.
	 * @param idApplicationResponsibleParam The value for the attribute {@link #idApplicationResponsible}.
	 */
	// CHECKSTYLE:OFF -- Checkstyle rule "Design for Extension" is not applied
	// because Hibernate JPA needs not final access methods.
	public void setIdApplicationResponsible(ApplicationResponsiblePK idApplicationResponsibleParam) {
		// CHECKSTYLE:ON
		this.idApplicationResponsible = idApplicationResponsibleParam;
	}

	/**
	 * Gets the value of the attribute {@link #application}.
	 * @return the value of the attribute {@link #application}.
	 */
	// CHECKSTYLE:OFF -- Checkstyle rule "Design for Extension" is not applied
	// because Hibernate JPA needs not final access methods.
	@MapsId("idApplication")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_aplicaciones", nullable = false, insertable = false, updatable = false)
	public Application getApplication() {
		// CHECKSTYLE:ON
		return application;
	}

	/**
	 * Sets the value of the attribute {@link #application}.
	 * @param applicationParam The value for the attribute {@link #application}.
	 */
	// CHECKSTYLE:OFF -- Checkstyle rule "Design for Extension" is not applied
	// because Hibernate JPA needs not final access methods.
	public void setApplication(Application applicationParam) {
		// CHECKSTYLE:ON
		this.application = applicationParam;
	}

	/**
	 * Gets the value of the attribute {@link #responsible}.
	 * @return the value of the attribute {@link #responsible}.
	 */
	// CHECKSTYLE:OFF -- Checkstyle rule "Design for Extension" is not applied
	// because Hibernate JPA needs not final access methods.
	@MapsId("idResponsible")
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "id_responsables", nullable = false, insertable = false, updatable = false)
	public User getResponsible() {
		// CHECKSTYLE:ON
		return responsible;
	}

	/**
	 * Sets the value of the attribute {@link #responsible}.
	 * @param responsibleParam The value for the attribute {@link #responsible}.
	 */
	// CHECKSTYLE:OFF -- Checkstyle rule "Design for Extension" is not applied
	// because Hibernate JPA needs not final access methods.
	public void setResponsible(User responsibleParam) {
		// CHECKSTYLE:ON
		this.responsible = responsibleParam;
	}

}
