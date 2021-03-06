package com.software.bank.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.software.bank.dao.IDataBase;
import com.software.bank.dao.exception.DaoException;
import com.software.bank.service.exception.ServiceException;
import com.software.bank.service.model.Credit;
import com.software.bank.service.model.Debit;
import com.software.bank.view.IVisual;

public abstract class CreditAbstract implements ICreditLogic {
	
	private IVisual view;
	
	private IDataBase dataBase;
	
	abstract protected Debit getMinPayment(Credit currentCredit); 
	
	abstract protected String [] createPaymentSchedule(Credit credit);
	
	public IVisual getView() {
		return view;
	}

	public void setView(IVisual view) {
		this.view = view;
	}
	
	public IDataBase getDataBase() {
		return dataBase;
	}

	public void setDataBase(IDataBase dataBase) {
		this.dataBase = dataBase;
	}

	@Override
	public void addCredit() throws ServiceException {
		Credit credit = view.addCreditMenuView(); // get data from user
		if (isContractExist(credit)){ // check existing contract
			return;
		}
		showSchedulePayments(credit);
		saveCredit(credit);
		view.showSuccess();
	}
	
	@Override
	public void addDebit() throws ServiceException {
		String contractNumber = view.addPaymentContractView(); // get data from user
		Credit credit = getCredit(contractNumber);
		
		if (isContractNotFound(credit) || isContractClosed(credit)) {
			return;
		}
		Debit debit = getUserPayment(credit);
		saveDebit(debit);
		view.showSuccess();
	}
	
	private Debit getUserPayment(Credit credit){
		// calculate minimum payment
		Debit debit = getMinPayment(credit);
		// get user enter
		BigDecimal usersDebit = view.addPaymentSummaView(debit.getMinDebit().setScale(2, RoundingMode.HALF_UP).toString());
		debit.setCurrentDebit(usersDebit);

		BigDecimal newTotalDebit = setNewTotalDebit(credit, debit);
		debit.setTotalDebit(newTotalDebit);
		return debit;
	}
	
	private BigDecimal setNewTotalDebit(Credit credit, Debit debit) {
		BigDecimal currentDebit = debit.getCurrentDebit().subtract(debit.getPercent()).setScale(2, RoundingMode.HALF_UP);
		BigDecimal newTotalDebet = currentDebit.add(credit.getTotalDebit());
		return newTotalDebet;
	}
	
	private void showSchedulePayments(Credit credit){
		String [] paymentSchedule = createPaymentSchedule(credit);
		view.showPaymentSchedule(paymentSchedule);
	}
	
	private boolean isContractExist(Credit credit) throws ServiceException {
		if (getCredit(credit.getContractNumber()) != null){
			view.showContractIsExist();
			return true;
		}
		return false;
	}
	
	private boolean isContractNotFound(Credit credit) {
		if (credit == null){
			view.showContractNotFound();
			return true;
		}
		return false;
	}
	
	private boolean isContractClosed(Credit credit) {
		if (credit.getTerm() == credit.getQtyPayments()){
			view.showContractClosed();
			return true;
		}
		return false;
	}
	
	private void saveDebit(Debit debit) throws ServiceException {
		try {
			dataBase.addPayment(debit.getContractNumber(), debit.getTotalDebit());
		} catch (DaoException e) {
			throw new ServiceException(e);
		}
	}

	private void saveCredit(Credit credit) throws ServiceException {
		try {
			dataBase.addCredit(credit);
		} catch (DaoException e) {
			throw new ServiceException(e);
		}
	}
	
	private Credit getCredit(String contractNumber) throws ServiceException {
		try {
			Credit credit = dataBase.getCredit(contractNumber);
			return credit;
		} catch (DaoException e) {
			throw new ServiceException(e);
		}
	}
}