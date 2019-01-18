import React, { Component } from 'react';

class Export extends Component {
  constructor(props) {
    super(props);
    console.log('Export props:', props);
    this.state = { label: props.label, transactions: props.transactions, compoundApi: props.compoundApi };
    this.formatTransaction = this.formatTransaction.bind(this);
  }

  componentWillReceiveProps(newProps){
    this.setState({
      label: newProps.label,
      transactions: newProps.transactions
    });
  }

  render() {
    console.log('Period render: number of transactions:', this.state.transactions.length);
    const header = 'Datum,Naam / Omschrijving,Pot,Rekening,Tegenrekening,Code,Bedrag (EUR),Mutatie-soort,Mededelingen';
    const rows = this.state.transactions.map(this.formatTransaction).join('\n');
    const exportData = [header, rows].join('\n');
    console.log('Export render: rows:', rows);
    return (
      <textarea>
{exportData}
</textarea>
    );
  }

  formatTransaction(record) {
    // console.log('Record:', record);
    const self = this;
    const keys = [ 'date', 'name', 'intendedJar', 'account', 'contraAccount', 'code', 'signedCents', 'kind', 'remarks' ]
    const cells = keys
      .map(
        (key) => {
          var value;
          if (key === 'intendedJar') {
            if (record.intendedJar) {
              value = record.intendedJar;
              if (record.intendedJar === '*') {
                value = '*';
              } else if (record.balanceValid === 'yes') {
                value = '+' + value;
              } else {
                value = '-' + value;
              }
            } else {
              value = '?';
            }
          } else if (!record.hasOwnProperty(key)) {
            value = '???';
          } else if (key === 'signedCents') {
            value = self.formatValue(record[key])
          } else if (key === 'account' || key === 'contraAccount') {
            value = self.formatAccount(record[key])
          } else {
            value = '' + record[key];
          }
          return '"' + value.replace(/\\/, '\\\\\\\\').replace('"', '\\\\"') + '"';
        }
      )
    ;
    return cells.join(',');
  }

  formatValue(value) {
    var prefix = '';
    var amount = value;
    if (amount < 0) {
      prefix = '-';
      amount = -amount;
    }
    const intPart = '' + Math.floor(amount / 100)
    var fraction = ('' + (100 + (amount % 100))).substring(1)
    return prefix + intPart + ',' + fraction;
  }

  formatAccount(value) {
    var accounts = this.state.compoundApi.getAccounts();
    if (accounts.hasOwnProperty(value)) {
      return accounts[value].label;
    } else {
      return value;
    }
  }

  accountJar(value) {
    var accounts = this.state.compoundApi.getAccounts();
    if (accounts.hasOwnProperty(value)) {
      return accounts[value].key;
    } else {
      return '*';
    }
  }
}

export default Export;