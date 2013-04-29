"use strict";

COI.module("Cheques", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Cheque = Backbone.Model.extend({
		defaults: {
			valor: null,
			data: null,
			paciente: null
		},
		parse: function(resp, options) {
			resp.data = $.datepicker.formatDate('dd/mm/yy', $.datepicker.parseDate('yy-mm-dd', resp.data));
			return resp;
		}
	});

	var Cheques = Backbone.Collection.extend({
		url: '/rest/cheques',
		model: Cheque
	});
	
	var RowView = Marionette.ItemView.extend({
		template: '#cheque_row_template',
		tagName: 'tr',
		events: {
			'click .coi-action-update': 'doUpdate',
			'click .coi-action-delete': 'doDelete'
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.$el.form();
		},
		doUpdate: function(e) {
			e.preventDefault();
			Backbone.history.navigate('cheque/' + this.model.get('id'), true);
		},
		doDelete: function(e) {
			e.preventDefault();
			
			var model = this.model;
			_promptDelete(function() {
				model.destroy({
					wait: true,
					success: function(model, response, options) {
						_notifyDelete();
					},
					error: function(model, xhr, options) {
						_notifyDeleteFailure();
					}
				});
			});
		}
	});
	
	var ChequesView = Marionette.CompositeView.extend({
		template: '#cheques_template',
		tagName: 'form',
		itemViewContainer: 'tbody',
		itemView: RowView,
		events: {
			'click .coi-action-cancel': 'doCancel',
			'click .coi-action-create': 'doCreate'
		},
		ui: {
			'table': 'table'
		},
		initialize: function() {
			_.bindAll(this);
			this.collection.fetch();
		},
		onRender: function() {
			this.$el.form();
			this.ui.table.table().css('width', '100%');
		},
		doCancel: function(e) {
			e.preventDefault();
			Backbone.history.navigate('', true);
		},
		doCreate: function(e) {
			e.preventDefault();
			Backbone.history.navigate('cheque', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('cheques', function() {
			COI.body.show(new ChequesView({collection: new Cheques()}));
		});
	});
});