package es.gob.fire.statistics.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import es.gob.fire.statistics.config.DBConnectionException;
import es.gob.fire.statistics.config.DbManager;
import es.gob.fire.statistics.entity.AuditTransactionCube;
import es.gob.fire.statistics.entity.TransactionTotal;

public class AuditTransactionsDAO {
	
	/** SQL para insertar una peticion. */
	private static final String ST_INSERT_AUDIT_TRANS = "INSERT INTO TB_AUDIT_TRANSACCIONES " //$NON-NLS-1$
			+ "(fecha, id_aplicacion, nombre_aplicacion, id_transaccion, operacion, operacion_criptografica, formato, formato_actualizado, algoritmo, proveedor, proveedor_forzado, navegador, tamanno, resultado, error_detalle, nodo) " //$NON-NLS-1$
			+ "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; //$NON-NLS-1$

	/**
	 * Inserta una configuraci&oacute;n de operaci&oacute;n de firma en base de datos indicando
	 * cuantas veces se dio esta configuraci&oacute;n un d&iacute;a concreto.
	 * @param transaction Configuraci&oacute;n de la operaci&oacute;n de firma.
	 * @return {@code true} si la configuraci&oacute;n se inserto correctamente. {@code false}
	 * en caso contrario.
	 * @throws SQLException Cuando se produce un error al insertar los datos.
	 * @throws DBConnectionException Cuando se produce un error de conexi&oacute;n con la base de datos.
	 */
	public static boolean insertAuditTransaction(final AuditTransactionCube transaction, final TransactionTotal total)
			throws SQLException, DBConnectionException {
		boolean inserted = false;
		Connection conn = null;
		try {
			conn = DbManager.getInstance().getConnection(false);
			inserted = insertAuditTransaction(transaction, total, conn);
			
			conn.commit();
		} catch (SQLException e) {
			throw e;
		} finally {
			if (conn != null){
				try {
					conn.close();
				} catch (SQLException e) {
					throw e;
				}
			}
		}
		
		return inserted;
	}
	
	/**
	 * Inserta una configuraci&oacute;n de operaci&oacute;n de firma en base de datos indicando
	 * cuantas veces se dio esta configuraci&oacute;n un d&iacute;a concreto.
	 * @param transaction Configuraci&oacute;n de la operaci&oacute;n de firma.
	 * @return {@code true} si la configuraci&oacute;n se inserto correctamente. {@code false}
	 * en caso contrario.
	 * @throws SQLException Cuando se produce un error al insertar los datos.
	 * @throws DBConnectionException Cuando se produce un error de conexi&oacute;n con la base de datos.
	 */
	public static boolean insertAuditTransaction(final AuditTransactionCube transaction, final TransactionTotal total,
			final Connection conn)
			throws SQLException, DBConnectionException {

		try (final PreparedStatement st = conn.prepareStatement(ST_INSERT_AUDIT_TRANS)) {
			st.setTimestamp (1, new java.sql.Timestamp(transaction.getDate().getTime()));
			st.setString(2, transaction.getIdApplication());
			st.setString(3, transaction.getNameApplication());
			st.setString(4, transaction.getIdTransaction());
			st.setString(5, transaction.getOperation());
			st.setString(6, transaction.getCryptoOperation());
			st.setString(7, transaction.getFormat());
			st.setString(8, transaction.getImprovedFormat());
			st.setString(9, transaction.getAlgorithm());
			st.setString(10, transaction.getProvider());
			st.setBoolean(11, transaction.isMandatoryProvider());
			st.setString(12, transaction.getBrowser());
			st.setLong(13, total.getDataSize());
			st.setBoolean(14, transaction.isResult());
			st.setString(15, transaction.getErrorDetail());
			st.setString(16, transaction.getNode());
			if (st.executeUpdate() < 1) {
				return false;
			}
		}
		return true;
	}
}
