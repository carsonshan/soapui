/*
 *  soapUI, copyright (C) 2004-2011 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.security.log;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import org.apache.log4j.Logger;

import com.eviware.soapui.impl.wsdl.testcase.TestCaseLogItem;
import com.eviware.soapui.model.security.SecurityCheck;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.security.SecurityTest;
import com.eviware.soapui.security.result.SecurityCheckRequestResult;
import com.eviware.soapui.security.result.SecurityCheckResult;
import com.eviware.soapui.security.result.SecurityResult;
import com.eviware.soapui.security.result.SecurityTestStepResult;
import com.eviware.soapui.security.result.SecurityResult.ResultStatus;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.swing.ActionList;
import com.eviware.soapui.support.action.swing.ActionSupport;
import com.eviware.soapui.support.components.JHyperlinkLabel;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;
import com.eviware.x.form.support.AField.AFieldType;

/**
 * Panel for displaying SecurityChecks Results
 * 
 * @author dragica.soldo
 */

public class JSecurityTestRunLog extends JPanel
{
	private SecurityTestLogModel logListModel;
	private JList testLogList;
	private boolean errorsOnly = false;
	private final Settings settings;
	private Set<String> boldTexts = new HashSet<String>();
	private boolean follow = true;
	protected int selectedIndex;
	private XFormDialog optionsDialog;
	private Logger log = Logger.getLogger( JSecurityTestRunLog.class );

	public JSecurityTestRunLog( SecurityTest securityTest )
	{
		super( new BorderLayout() );
		this.settings = securityTest.getSettings();
		logListModel = securityTest.getSecurityTestLog();
		errorsOnly = settings.getBoolean( OptionsForm.class.getName() + "@errors_only" );
		buildUI();
	}

	private void buildUI()
	{
		logListModel = new SecurityTestLogModel();
		logListModel.setMaxSize( ( int )settings.getLong( OptionsForm.class.getName() + "@max_rows", 1000 ) );

		testLogList = new JList( logListModel );
		testLogList.setCellRenderer( new SecurityTestLogCellRenderer() );
		testLogList.addMouseListener( new LogListMouseListener() );

		JScrollPane scrollPane = new JScrollPane( testLogList );

		add( scrollPane, BorderLayout.CENTER );
		add( buildToolbar(), BorderLayout.NORTH );
	}

	private Component buildToolbar()
	{
		JXToolBar toolbar = UISupport.createSmallToolbar();

		addToolbarButtons( toolbar );

		return toolbar;
	}

	protected JList getTestLogList()
	{
		return testLogList;
	}

	public boolean isErrorsOnly()
	{
		return errorsOnly;
	}

	public boolean isFollow()
	{
		return follow;
	}

	protected void addToolbarButtons( JXToolBar toolbar )
	{
		toolbar.addFixed( UISupport.createToolbarButton( new ClearLogAction() ) );
		toolbar.addFixed( UISupport.createToolbarButton( new SetLogOptionsAction() ) );
		toolbar.addFixed( UISupport.createToolbarButton( new ExportLogAction() ) );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.eviware.soapui.impl.wsdl.panels.testcase.TestRunLog#clear()
	 */
	public synchronized void clear()
	{
		logListModel.clear();
		boldTexts.clear();
	}

	public synchronized void locateSecurityCheck( SecurityCheck check )
	{
		try
		{
			int idx = logListModel.getIndexOfSecurityCheck( check );
			if( idx != -1 )
			{
				testLogList.ensureIndexIsVisible( idx );
			}
		}
		catch( RuntimeException e )
		{
		}
	}

	public synchronized boolean addSecurityTestStepResult( TestStep testStep )
	{
		boolean added = logListModel.addSecurityTestStepResult( testStep );
		if( follow )
		{
			try
			{
				testLogList.ensureIndexIsVisible( logListModel.getSize() - 1 );
			}
			catch( RuntimeException e )
			{
				log.error( e.getMessage() );
			}
		}
		return added;
	}

	public synchronized void updateSecurityTestStepResult( SecurityTestStepResult testStepResult,
			boolean hasChecksToProcess, boolean startStepLogEntryAdded )
	{
		logListModel
				.updateSecurityTestStepResult( testStepResult, errorsOnly, hasChecksToProcess, startStepLogEntryAdded );
		if( follow )
		{
			try
			{
				testLogList.ensureIndexIsVisible( logListModel.getSize() - 1 );
			}
			catch( RuntimeException e )
			{
				log.error( e.getMessage() );
			}
		}
	}

	public synchronized void addSecurityCheckResult( SecurityCheck securityCheck )
	{
		logListModel.addSecurityCheckResult( securityCheck );
		if( follow )
		{
			try
			{
				testLogList.ensureIndexIsVisible( logListModel.getSize() - 1 );
			}
			catch( RuntimeException e )
			{
				log.error( e.getMessage() );
			}
		}
	}

	public synchronized void updateSecurityCheckResult( SecurityCheckResult checkResult )
	{
		logListModel.updateSecurityCheckResult( checkResult, errorsOnly );
		if( follow )
		{
			try
			{
				testLogList.ensureIndexIsVisible( logListModel.getSize() - 1 );
			}
			catch( RuntimeException e )
			{
				log.error( e.getMessage() );
			}
		}
	}

	public synchronized void addSecurityCheckRequestResult( SecurityCheckRequestResult checkRequestResult )
	{
		if( errorsOnly && checkRequestResult.getStatus() != SecurityCheckRequestResult.ResultStatus.FAILED )
			return;

		logListModel.addSecurityCheckRequestResult( checkRequestResult );
		if( follow )
		{
			try
			{
				testLogList.ensureIndexIsVisible( logListModel.getSize() - 1 );
			}
			catch( RuntimeException e )
			{
				log.error( e.getMessage() );
			}
		}
	}

	private class SetLogOptionsAction extends AbstractAction
	{
		public SetLogOptionsAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/options.gif" ) );
			putValue( Action.SHORT_DESCRIPTION, "Sets TestCase Log Options" );
		}

		public void actionPerformed( ActionEvent e )
		{
			if( optionsDialog == null )
				optionsDialog = ADialogBuilder.buildDialog( OptionsForm.class );

			optionsDialog.setIntValue( OptionsForm.MAXROWS, ( int )settings.getLong( OptionsForm.class.getName()
					+ "@max_rows", 1000 ) );
			optionsDialog.setBooleanValue( OptionsForm.ERRORSONLY, settings.getBoolean( OptionsForm.class.getName()
					+ "@errors_only" ) );
			optionsDialog.setBooleanValue( OptionsForm.FOLLOW, follow );

			if( optionsDialog.show() )
			{
				int maxRows = optionsDialog.getIntValue( OptionsForm.MAXROWS, 1000 );
				logListModel.setMaxSize( maxRows );
				settings.setLong( OptionsForm.class.getName() + "@max_rows", maxRows );
				errorsOnly = optionsDialog.getBooleanValue( OptionsForm.ERRORSONLY );
				settings.setBoolean( OptionsForm.class.getName() + "@errors_only", errorsOnly );

				follow = optionsDialog.getBooleanValue( OptionsForm.FOLLOW );
			}
		}
	}

	@AForm( name = "Log Options", description = "Set options for the run log below" )
	private static interface OptionsForm
	{
		@AField( name = "Max Rows", description = "Sets the maximum number of rows to keep in the log", type = AFieldType.INT )
		public static final String MAXROWS = "Max Rows";

		@AField( name = "Errors Only", description = "Logs only TestStep errors in the log", type = AFieldType.BOOLEAN )
		public static final String ERRORSONLY = "Errors Only";

		@AField( name = "Follow", description = "Follow log content", type = AFieldType.BOOLEAN )
		public static final String FOLLOW = "Follow";
	}

	private class ClearLogAction extends AbstractAction
	{
		public ClearLogAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/clear_loadtest.gif" ) );
			putValue( Action.SHORT_DESCRIPTION, "Clears the log" );
		}

		public void actionPerformed( ActionEvent e )
		{
			logListModel.clear();
		}
	}

	private class ExportLogAction extends AbstractAction
	{
		public ExportLogAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/export.gif" ) );
			putValue( Action.SHORT_DESCRIPTION, "Exports this log to a file" );
		}

		public void actionPerformed( ActionEvent e )
		{
			File file = UISupport.getFileDialogs().saveAs( this, "Save Log" );
			if( file != null )
			{
				try
				{
					PrintWriter out = new PrintWriter( file );
					printLog( out );

					out.close();
				}
				catch( FileNotFoundException e1 )
				{
					UISupport.showErrorMessage( e1 );
				}
			}
		}
	}

	public void release()
	{
		if( optionsDialog != null )
		{
			optionsDialog.release();
			optionsDialog = null;
		}
	}

	public void printLog( PrintWriter out )
	{
		for( int c = 0; c < logListModel.getSize(); c++ )
		{
			Object value = logListModel.getElementAt( c );
			if( value instanceof String )
			{
				out.println( value.toString() );
			}
		}
	}

	/**
	 * Mouse Listener for triggering default action and showing popup for log
	 * list items
	 * 
	 * @author Ole.Matzura
	 */

	private final class LogListMouseListener extends MouseAdapter
	{
		public void mouseClicked( MouseEvent e )
		{
			int index = testLogList.getSelectedIndex();
			if( index != -1 && ( index == selectedIndex || e.getClickCount() > 1 ) )
			{
				SecurityResult result = logListModel.getTestStepResultAt( index );
				if( result != null && result.getActions() != null )
					result.getActions().performDefaultAction( new ActionEvent( this, 0, null ) );
			}
			selectedIndex = index;
		}

		public void mousePressed( MouseEvent e )
		{
			if( e.isPopupTrigger() )
				showPopup( e );
		}

		public void mouseReleased( MouseEvent e )
		{
			if( e.isPopupTrigger() )
				showPopup( e );
		}

		public void showPopup( MouseEvent e )
		{
			int row = testLogList.locationToIndex( e.getPoint() );
			if( row == -1 )
				return;

			if( testLogList.getSelectedIndex() != row )
			{
				testLogList.setSelectedIndex( row );
			}

			SecurityResult result = logListModel.getTestStepResultAt( row );
			if( result == null )
				return;

			ActionList actions = result.getActions();

			if( actions == null || actions.getActionCount() == 0 )
				return;

			JPopupMenu popup = ActionSupport.buildPopup( actions );
			UISupport.showPopup( popup, testLogList, e.getPoint() );
		}
	}

	public synchronized void addText( String string )
	{
		logListModel.addText( string );
		if( follow )
			testLogList.ensureIndexIsVisible( logListModel.getSize() - 1 );
	}

	private final class SecurityTestLogCellRenderer extends JLabel implements ListCellRenderer
	{
		private Font boldFont;
		private Font normalFont;
		private JHyperlinkLabel hyperlinkLabel = new JHyperlinkLabel( "" );

		public SecurityTestLogCellRenderer()
		{
			setOpaque( true );
			setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
			setIcon( null );
			boldFont = getFont().deriveFont( Font.BOLD );
			normalFont = getFont();

			hyperlinkLabel.setOpaque( true );
			hyperlinkLabel.setForeground( Color.BLUE.darker().darker().darker() );
			hyperlinkLabel.setUnderlineColor( Color.GRAY );
			hyperlinkLabel.setBorder( BorderFactory.createEmptyBorder( 0, 4, 3, 3 ) );
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus )
		{
			if( isSelected )
			{
				setBackground( list.getSelectionBackground() );
				setForeground( list.getSelectionForeground() );
			}
			else
			{
				setBackground( list.getBackground() );
				setForeground( list.getForeground() );
			}

			if( value instanceof String )
			{
				setText( value.toString() );
			}
			else if( value instanceof TestCaseLogItem )
			{
				TestCaseLogItem logItem = ( TestCaseLogItem )value;
				String msg = logItem.getMsg();
				setText( msg == null ? "" : msg );
			}

			SecurityResult result = logListModel.getTestStepResultAt( index );
			if( result != null )
			{
				if( result.getResultType().equals( SecurityCheckRequestResult.TYPE ) )
				{
					hyperlinkLabel.setText( getText() );
					hyperlinkLabel.setBackground( getBackground() );
					hyperlinkLabel.setEnabled( list.isEnabled() );
					hyperlinkLabel.setUnderlineColor( Color.WHITE );
					hyperlinkLabel.setIcon( null );

					hyperlinkLabel.setBorder( BorderFactory.createEmptyBorder( 0, 24, 3, 3 ) );
				}
				else if( result.getResultType().equals( SecurityCheckResult.TYPE ) )
				{
					hyperlinkLabel.setText( getText() );
					hyperlinkLabel.setBackground( getBackground() );
					hyperlinkLabel.setEnabled( list.isEnabled() );

					hyperlinkLabel.setBorder( BorderFactory.createEmptyBorder( 0, 16, 3, 3 ) );
					hyperlinkLabel.setUnderlineColor( Color.WHITE );
					hyperlinkLabel.setIcon( null );
					// if( getText().startsWith( "SecurityCheck" ) &&
					// !getText().startsWith( " ->" ) )
					// if( result.getStatus() != SecurityStatus.INITIALIZED )
					// {
					hyperlinkLabel.setUnderlineColor( Color.GRAY );
					setStatusIcon( result );

					// }
				}
				else if( result.getResultType().equals( SecurityTestStepResult.TYPE ) )
				{
					SecurityTestStepResult securitytestStepresult = ( SecurityTestStepResult )result;
					hyperlinkLabel.setText( getText() );
					hyperlinkLabel.setBackground( getBackground() );
					hyperlinkLabel.setEnabled( list.isEnabled() );
					hyperlinkLabel.setUnderlineColor( Color.WHITE );
					hyperlinkLabel.setIcon( null );

					if( getText().startsWith( "Step" ) && !getText().startsWith( " ->" ) )
					{
						hyperlinkLabel.setBorder( BorderFactory.createEmptyBorder( 0, 4, 3, 3 ) );
						hyperlinkLabel.setUnderlineColor( Color.GRAY );
						setStatusIcon( securitytestStepresult );
					}

					else
					{
						hyperlinkLabel.setBorder( BorderFactory.createEmptyBorder( 0, 4, 3, 3 ) );
					}
				}
				return hyperlinkLabel;
			}
			setEnabled( list.isEnabled() );

			if( boldTexts.contains( getText() ) )
			{
				setFont( boldFont );
			}
			else
			{
				setFont( normalFont );
			}

			return this;
		}

		private void setStatusIcon( SecurityResult securityrresult )
		{
			if( securityrresult.getStatus() == ResultStatus.OK )
			{
				hyperlinkLabel.setIcon( UISupport.createImageIcon( "/valid_assertion.gif" ) );
			}
			else if( securityrresult.getStatus() == ResultStatus.FAILED )
			{
				hyperlinkLabel.setIcon( UISupport.createImageIcon( "/failed_assertion.gif" ) );
			}
			else
			{
				hyperlinkLabel.setIcon( UISupport.createImageIcon( "/unknown_assertion.gif" ) );
			}
		}
	}

}
