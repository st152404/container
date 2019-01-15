/**
 * 
 */
package org.opentosca.planbuilder.model.tosca;

import java.util.List;

import javax.xml.namespace.QName;

/**
 * @author Kalman Kepes - kepes@iaas.uni-stuttgart.de
 *
 */
public abstract class AbstractGroup {

	/**
	 * Returns the id
	 * 
	 * @return a String containing the local if of this groups
	 */
	public abstract String getId();

	/**
	 * Returns the namespace of this group
	 * 
	 * @return a String containing the namespace
	 */
	public abstract String getTargetNamespace();

	/**
	 * Returns the QName of this group
	 * 
	 * @return a QName
	 */
	public abstract QName getQName();

	/**
	 * Returns the type of this group
	 * 
	 * @return a QName containing the type of this group
	 */
	public abstract QName getType();

	/**
	 * Returns the properties of this group
	 * 
	 * @return an AbstractProperties object
	 */
	public abstract AbstractProperties getProperties();

	/**
	 * Returns the Node Templates inside this group
	 * 
	 * @return a List of AbstractNodeTemplate
	 */
	public abstract List<AbstractNodeTemplate> getNodeTemplates();

}
