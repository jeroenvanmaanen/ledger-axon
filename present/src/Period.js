import React, { Component } from 'react';

class Period extends Component {
  constructor(props) {
    super(props);
    console.log('Period props:', props);
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
    const rows = this.state.transactions.map(this.formatTransaction);
    return (
      <div className="Period">
        <h2 className="Period-title">{this.state.label}</h2>
        <table>
          <colgroup>
            <col width="45px"/>
            <col width="90px"/>
            <col width="1*"/>
            <col width="60px"/>
            <col width="180px"/>
            <col width="180px"/>
            <col width="45px"/>
            <col width="70px"/>
            <col width="130px"/>
            <col width="3*"/>
          </colgroup>
          <thead>
            <tr>
              <th>Add</th>
              <th>Datum</th>
              <th>Naam / Omschrijving</th>
              <th>Pot</th>
              <th>Rekening</th>
              <th>Tegenrekening</th>
              <th>Code</th>
              <th className="key_amountCents">Bedrag (EUR)</th>
              <th>Mutatie-soort</th>
              <th>Mededelingen</th>
            </tr>
          </thead>
          <tbody>
            {rows}
          </tbody>
        </table>
      </div>
    );
  }

  formatTransaction(record) {
    // console.log('Record:', record);
    const self = this;
    const keys = [ 'add', 'date', 'description', 'intendedJar', 'account', 'contraAccount', 'code', 'amountCents', 'kind', 'remarks' ]
    const cells = keys
      .map(
        (key) => {
          var value;
          var cssClass = 'key_'+key;
          var unformatted = undefined;
          var jar = undefined;
          if (key === 'add') {
            value = '+';
          } else if (key === 'intendedJar') {
            if (record.intendedJar) {
              value = record.intendedJar;
              if (record.intendedJar === '?') {
                cssClass = cssClass + ' todo';
              } else if (record.intendedJar === '*' || record.balanceMatchesIntention) {
                cssClass = cssClass + ' valid';
              } else {
                cssClass = cssClass + ' invalid';
              }
            } else {
              value = '?';
              cssClass = cssClass + ' todo';
            }
          } else if (!record.hasOwnProperty(key)) {
            value = '???';
          } else if (key === 'amountCents') {
            unformatted = record[key]
            value = self.formatValue(unformatted)
          } else if (key === 'account' || key === 'contraAccount') {
            unformatted = record[key]
            jar = self.accountJar(unformatted)
            value = self.formatAccount(unformatted)
          } else if (key === 'remarks') {
            const remarksText = record[key] || '';
            var parts = [];
            var i = 0;
            for (const match of remarksText.matchAll(/[0-9]{4}-[01][0-9]-[0-3][0-9]/g)) {
                if (match.index > i) {
                    parts.push((<span data-id={i}>{remarksText.substring(i, match.index)}</span>));
                }
                parts.push((<span class='link' onClick={this.state.compoundApi.goBack} data-id={match.index}>{match[0]}</span>));
                i = match.index + match[0].length;
            }
            if (i < 1) {
              value = remarksText;
            } else {
              if (remarksText.length > i) {
                parts.push((<span data-id={i}>{remarksText.substring(i)}</span>));
              }
              value = (parts);
            }
          } else {
            value = '' + record[key];
          }
          if (unformatted === undefined) {
            return <td key={key} className={cssClass} onClick={self.state.compoundApi.changeFocus}>{value}</td>;
          } else if (jar === undefined) {
            return <td key={key} className={cssClass} onClick={self.state.compoundApi.changeFocus} unformatted={'' + unformatted}>{value}</td>;
          } else {
            return <td key={key} className={cssClass} onClick={self.state.compoundApi.changeFocus} unformatted={'' + unformatted} jar={jar}>{value}</td>;
          }
        }
      )
    ;
    var cssClass = 'transaction';
    const d1 = self.getDepth(record.account);
    const d2 = self.getDepth(record.contraAccount);
    if (d1 * d2 > 0 && record.amountCents >= 0) {
      cssClass = cssClass + ' hidden';
    }
    if (this.state.compoundApi.isMember(record.key)) {
      cssClass = cssClass + ' compoundMember';
    }
    return <tr key={record.id} className={cssClass} data-id={record.id} data-key={record.key}>{cells}</tr>;
  }

  getDepth(account) {
    const self = this;
    const accountsMap = self.state.compoundApi.getAccounts();
    if (accountsMap.hasOwnProperty(account)) {
      return accountsMap[account].depth;
    } else {
      return 0;
    }
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

export default Period;